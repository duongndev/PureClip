package com.pureclip.app.ui.result

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.pureclip.app.R
import com.pureclip.app.data.model.VideoInfo
import com.pureclip.app.databinding.ActivityResultBinding
import com.pureclip.app.utils.AdsManager
import com.pureclip.app.utils.DownloadHelper
import com.pureclip.app.utils.NetworkUtils
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: ResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.resultRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load Banner Ad
        AdsManager.loadBannerAd(this, binding.adContainer)

        setupListeners()
        observeViewModel()
        checkAndInitData()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun checkAndInitData() {
        // If state is not already initialized by SavedStateHandle, load from intent
        if (viewModel.uiState.value is ResultUiState.Loading) {
            val videoInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(ResultViewModel.EXTRA_VIDEO_INFO, VideoInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(ResultViewModel.EXTRA_VIDEO_INFO) as? VideoInfo
            }

            if (videoInfo != null) {
                viewModel.setVideoInfo(videoInfo)
            } else {
                Toast.makeText(this, "Không tìm thấy thông tin video", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ResultUiState.Loading -> {
                                // Loading or waiting for data
                            }
                            is ResultUiState.Error -> {
                                Toast.makeText(this@ResultActivity, state.message, Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            is ResultUiState.Success -> {
                                renderSuccess(state)
                            }
                        }
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun renderSuccess(state: ResultUiState.Success) {
        binding.tvTitle.text = state.title

        // Thumbnail
        if (state.thumbnailUrl.isNotEmpty()) {
            Glide.with(this)
                .load(state.thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_thumbnail_placeholder)
                .error(R.drawable.bg_thumbnail_placeholder)
                .into(binding.ivThumbnail)
        }

        // Render Download Options
        binding.linearDownloadsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (option in state.downloadOptions) {
            val itemView = inflater.inflate(R.layout.item_download_option, binding.linearDownloadsContainer, false)

            val tvQualityTitle = itemView.findViewById<TextView>(R.id.tvQualityTitle)
            val tvDownloadDetails = itemView.findViewById<TextView>(R.id.tvDownloadDetails)
            val btnDownloadItem = itemView.findViewById<MaterialButton>(R.id.btnDownloadItem)
            val ivDownloadIcon = itemView.findViewById<ImageView>(R.id.ivDownloadIcon)

            tvQualityTitle.text = option.title
            tvDownloadDetails.text = option.details
            ivDownloadIcon.setImageResource(option.iconRes)

            btnDownloadItem.setOnClickListener {
                viewModel.onDownloadOptionClicked(option)
            }

            binding.linearDownloadsContainer.addView(itemView)
        }
    }

    private fun handleEvent(event: ResultEvent) {
        when (event) {
            is ResultEvent.StartDownload -> {
                val option = event.option
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    Toast.makeText(this, getString(R.string.toast_no_internet), Toast.LENGTH_SHORT).show()
                    return
                }
                AdsManager.showInterstitial(this) {
                    DownloadHelper.downloadVideo(
                        this,
                        option.downloadItem.url,
                        option.videoTitle,
                        option.platform,
                        option.downloadItem.format ?: "mp4"
                    )
                }
            }
            is ResultEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AdsManager.resumeBanner(binding.adContainer)
    }

    override fun onPause() {
        super.onPause()
        AdsManager.pauseBanner(binding.adContainer)
    }

    override fun onDestroy() {
        AdsManager.destroyBanner(binding.adContainer)
        super.onDestroy()
    }
}
