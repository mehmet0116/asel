package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aikodasistani.aikodasistani.util.DeveloperToolsUtil
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText

/**
 * Activity for Developer Tools - JSON, Base64, Regex, Hash utilities
 */
class DeveloperToolsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_tools)

        setupToolbar()
        setupViewPager()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = DevToolsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.json_tool)
                1 -> getString(R.string.base64_tool)
                2 -> getString(R.string.regex_tool)
                3 -> getString(R.string.hash_tool)
                else -> ""
            }
        }.attach()
    }

    class DevToolsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> JsonToolFragment()
                1 -> Base64ToolFragment()
                2 -> RegexToolFragment()
                3 -> HashToolFragment()
                else -> JsonToolFragment()
            }
        }
    }

    // ==================== JSON TOOL FRAGMENT ====================
    class JsonToolFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_json_tool, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val etInput = view.findViewById<TextInputEditText>(R.id.etInput)
            val tvOutput = view.findViewById<TextView>(R.id.tvOutput)
            val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
            val btnValidate = view.findViewById<Button>(R.id.btnValidate)
            val btnMinify = view.findViewById<Button>(R.id.btnMinify)
            val btnCopy = view.findViewById<Button>(R.id.btnCopy)

            btnValidate.setOnClickListener {
                val input = etInput.text?.toString() ?: ""
                if (input.isBlank()) {
                    tvStatus.visibility = View.GONE
                    return@setOnClickListener
                }

                val result = DeveloperToolsUtil.validateAndFormatJson(input)
                tvStatus.visibility = View.VISIBLE

                if (result.isValid) {
                    tvStatus.text = getString(R.string.json_valid)
                    tvStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
                    tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    tvOutput.text = result.formattedJson
                } else {
                    tvStatus.text = "${getString(R.string.json_invalid)}\n${result.errorMessage}"
                    tvStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error))
                    tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    tvOutput.text = input
                }
            }

            btnMinify.setOnClickListener {
                val input = etInput.text?.toString() ?: ""
                val minified = DeveloperToolsUtil.minifyJson(input)
                if (minified != null) {
                    tvOutput.text = minified
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = getString(R.string.json_valid)
                    tvStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
                    tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = getString(R.string.json_invalid)
                    tvStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error))
                    tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }

            btnCopy.setOnClickListener {
                copyToClipboard(requireContext(), tvOutput.text.toString())
            }
        }
    }

    // ==================== BASE64 TOOL FRAGMENT ====================
    class Base64ToolFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_base64_tool, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val etInput = view.findViewById<TextInputEditText>(R.id.etInput)
            val tvOutput = view.findViewById<TextView>(R.id.tvOutput)
            val btnEncode = view.findViewById<Button>(R.id.btnEncode)
            val btnDecode = view.findViewById<Button>(R.id.btnDecode)
            val btnCopy = view.findViewById<Button>(R.id.btnCopy)

            btnEncode.setOnClickListener {
                val input = etInput.text?.toString() ?: ""
                val result = DeveloperToolsUtil.encodeToBase64(input)
                tvOutput.text = result.result ?: result.errorMessage
            }

            btnDecode.setOnClickListener {
                val input = etInput.text?.toString() ?: ""
                val result = DeveloperToolsUtil.decodeFromBase64(input)
                tvOutput.text = result.result ?: result.errorMessage
            }

            btnCopy.setOnClickListener {
                copyToClipboard(requireContext(), tvOutput.text.toString())
            }
        }
    }

    // ==================== REGEX TOOL FRAGMENT ====================
    class RegexToolFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_regex_tool, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val etPattern = view.findViewById<TextInputEditText>(R.id.etPattern)
            val etTestString = view.findViewById<TextInputEditText>(R.id.etTestString)
            val tvMatchCount = view.findViewById<TextView>(R.id.tvMatchCount)
            val tvMatches = view.findViewById<TextView>(R.id.tvMatches)
            val btnTest = view.findViewById<Button>(R.id.btnTest)

            // Common pattern chips
            view.findViewById<Chip>(R.id.chipEmail).setOnClickListener {
                etPattern.setText(DeveloperToolsUtil.CommonPatterns.EMAIL)
            }
            view.findViewById<Chip>(R.id.chipUrl).setOnClickListener {
                etPattern.setText(DeveloperToolsUtil.CommonPatterns.URL)
            }
            view.findViewById<Chip>(R.id.chipPhone).setOnClickListener {
                etPattern.setText(DeveloperToolsUtil.CommonPatterns.PHONE)
            }
            view.findViewById<Chip>(R.id.chipIp).setOnClickListener {
                etPattern.setText(DeveloperToolsUtil.CommonPatterns.IP_ADDRESS)
            }
            view.findViewById<Chip>(R.id.chipHexColor).setOnClickListener {
                etPattern.setText(DeveloperToolsUtil.CommonPatterns.HEX_COLOR)
            }

            btnTest.setOnClickListener {
                val pattern = etPattern.text?.toString() ?: ""
                val testString = etTestString.text?.toString() ?: ""

                if (pattern.isBlank()) {
                    Toast.makeText(requireContext(), "Pattern giriniz", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val result = DeveloperToolsUtil.testRegex(pattern, testString)

                if (!result.isValid) {
                    tvMatchCount.text = getString(R.string.regex_invalid, result.errorMessage)
                    tvMatchCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                    tvMatches.text = ""
                    return@setOnClickListener
                }

                if (result.matches.isEmpty()) {
                    tvMatchCount.text = getString(R.string.no_matches)
                    tvMatchCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    tvMatches.text = getString(R.string.output_placeholder)
                } else {
                    tvMatchCount.text = getString(R.string.match_count, result.matches.size)
                    tvMatchCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))

                    val matchesText = StringBuilder()
                    result.matches.forEachIndexed { index, match ->
                        matchesText.append("${index + 1}. \"${match.fullMatch}\" (${match.startIndex}-${match.endIndex})\n")
                        if (match.groups.size > 1) {
                            match.groups.forEachIndexed { groupIndex, group ->
                                if (groupIndex > 0 && group != null) {
                                    matchesText.append("   Group $groupIndex: \"$group\"\n")
                                }
                            }
                        }
                    }
                    tvMatches.text = matchesText.toString().trim()
                }
            }
        }
    }

    // ==================== HASH TOOL FRAGMENT ====================
    class HashToolFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_hash_tool, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val etInput = view.findViewById<TextInputEditText>(R.id.etInput)
            val hashResultsContainer = view.findViewById<LinearLayout>(R.id.hashResultsContainer)
            val tvMd5 = view.findViewById<TextView>(R.id.tvMd5)
            val tvSha1 = view.findViewById<TextView>(R.id.tvSha1)
            val tvSha256 = view.findViewById<TextView>(R.id.tvSha256)
            val btnGenerate = view.findViewById<Button>(R.id.btnGenerate)

            val btnCopyMd5 = view.findViewById<ImageButton>(R.id.btnCopyMd5)
            val btnCopySha1 = view.findViewById<ImageButton>(R.id.btnCopySha1)
            val btnCopySha256 = view.findViewById<ImageButton>(R.id.btnCopySha256)

            btnGenerate.setOnClickListener {
                val input = etInput.text?.toString() ?: ""
                if (input.isBlank()) {
                    Toast.makeText(requireContext(), "Metin giriniz", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                hashResultsContainer.visibility = View.VISIBLE
                tvMd5.text = DeveloperToolsUtil.md5(input)
                tvSha1.text = DeveloperToolsUtil.sha1(input)
                tvSha256.text = DeveloperToolsUtil.sha256(input)
            }

            btnCopyMd5.setOnClickListener { copyToClipboard(requireContext(), tvMd5.text.toString()) }
            btnCopySha1.setOnClickListener { copyToClipboard(requireContext(), tvSha1.text.toString()) }
            btnCopySha256.setOnClickListener { copyToClipboard(requireContext(), tvSha256.text.toString()) }
        }
    }

    companion object {
        fun copyToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Developer Tools", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }
}
