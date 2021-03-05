package net.xzos.upgradeall.ui.base.listdialog

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.databinding.RecyclerlistContentBinding

interface ListDialogPart {
    val adapter: DialogListAdapter<*, *, *>

    fun initBinding(contentBinding: RecyclerlistContentBinding) {
        contentBinding.apply {
            if (adapter.itemCount > 0) {
                rvList.adapter = adapter
                rvList.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            } else {
                vfContainer.displayedChild = 1
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }
}