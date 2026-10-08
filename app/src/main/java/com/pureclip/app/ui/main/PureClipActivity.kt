package com.pureclip.app.ui.main

import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pureclip.app.R
import com.pureclip.app.databinding.ActivityMainBinding
import com.pureclip.app.ui.result.ResultActivity
import com.pureclip.app.ui.result.ResultViewModel
import com.pureclip.app.utils.AdsManager
import com.pureclip.app.utils.KeyboardUtils
import com.pureclip.app.utils.NetworkUtils
import kotlinx.coroutines.launch

/**
 * Entry point of PureClip main screen adhering to MVVM architecture.
 */
class PureClipActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PureClipViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // Initialize Ads & Banner
        AdsManager.initialize(this)
        AdsManager.loadBannerAd(this, binding.adContainer)

        setupButtonListeners()
        observeViewModel()
    }

    private fun setupButtonListeners() {
        binding.btnPaste.setOnClickListener {
            pasteFromClipboard()
        }

        binding.btnClear.setOnClickListener {
            binding.etUrl.setText("")
        }

        binding.btnFetch.setOnClickListener {
            clearInputFocusAndHideKeyboard()
            val url = binding.etUrl.text?.toString().orEmpty()
            if (!NetworkUtils.isNetworkAvailable(this)) {
                viewModel.setNoInternet()
                return@setOnClickListener
            }
            viewModel.fetchVideo(url)
        }

        binding.btnRetry.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, getString(R.string.toast_no_internet), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.retry()
        }

        binding.btnPasteOther.setOnClickListener {
            binding.etUrl.setText("")
            viewModel.resetState()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUIState(state)
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

    private fun updateUIState(state: MainUiState) {
        binding.layoutIdle.visibility = if (state is MainUiState.Idle) View.VISIBLE else View.GONE
        binding.layoutLoading.visibility = if (state is MainUiState.Loading) View.VISIBLE else View.GONE
        binding.layoutError.visibility = if (state is MainUiState.Error || state is MainUiState.NoInternet) View.VISIBLE else View.GONE

        when (state) {
            is MainUiState.NoInternet -> {
                binding.flErrorIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1F757575"))
                binding.ivErrorIcon.setImageResource(R.drawable.ic_wifi_off)
                binding.ivErrorIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary))
                binding.tvErrorTitle.text = getString(R.string.error_no_internet_title)
                binding.tvErrorMessage.text = getString(R.string.error_no_internet_message)
            }
            is MainUiState.Error -> {
                binding.flErrorIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1AD50000"))
                binding.ivErrorIcon.setImageResource(R.drawable.ic_error)
                binding.ivErrorIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error))
                binding.tvErrorTitle.text = getString(R.string.error_title)
                binding.tvErrorMessage.text = state.message?.ifBlank { getString(R.string.error_message) } ?: getString(R.string.error_message)
            }
            else -> Unit
        }
    }

    private fun handleEvent(event: MainEvent) {
        when (event) {
            is MainEvent.NavigateToResult -> {
                Toast.makeText(this, getString(R.string.toast_fetch_success), Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra(ResultViewModel.EXTRA_VIDEO_INFO, event.videoInfo)
                }
                startActivity(intent)
            }
            is MainEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty()) {
                binding.etUrl.setText(text)
            } else {
                Toast.makeText(this, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_no_content), Toast.LENGTH_SHORT).show()
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                val targetContainer = if (v.id == binding.etUrl.id) binding.tilUrl else v
                targetContainer.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    clearInputFocusAndHideKeyboard()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun clearInputFocusAndHideKeyboard() {
        binding.etUrl.clearFocus()
        binding.main.requestFocus()
        KeyboardUtils.hideKeyboard(this, binding.etUrl)
    }

    override fun onDestroy() {
        AdsManager.destroyBanner(binding.adContainer)
        super.onDestroy()
    }
}
