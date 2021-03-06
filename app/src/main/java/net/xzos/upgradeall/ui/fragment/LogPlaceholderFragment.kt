package net.xzos.upgradeall.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_log.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.ui.viewmodels.adapters.LogItemAdapter
import net.xzos.upgradeall.ui.viewmodels.viewmodel.LogPageViewModel


class LogPlaceholderFragment(
        private val bundleLogObjectTag: ObjectTag
) : Fragment() {

    private lateinit var mContext: Context
    private lateinit var logPageViewModel: LogPageViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_log, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = requireContext()
        logPageViewModel = ViewModelProvider(this).get(LogPageViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logPageViewModel.setLogObjectTag(bundleLogObjectTag)
        renewLogList()
    }

    private fun renewLogList() {
        val layoutManager = GridLayoutManager(mContext, 1)
        logListRecyclerView.layoutManager = layoutManager
        val adapter = LogItemAdapter(logPageViewModel.logList, this@LogPlaceholderFragment)
        logListRecyclerView.adapter = adapter
    }

    companion object {

        internal fun newInstance(logObjectTag: ObjectTag): LogPlaceholderFragment {
            return LogPlaceholderFragment(logObjectTag)
        }
    }
}
