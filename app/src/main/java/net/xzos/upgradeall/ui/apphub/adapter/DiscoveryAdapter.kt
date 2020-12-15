package net.xzos.upgradeall.ui.apphub.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.absinthe.libraries.utils.extensions.layoutInflater
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.server_manager.AppManager
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView

class DiscoveryAdapter : BaseQuickAdapter<CloudConfigListItemView, BaseViewHolder>(R.layout.item_hub_app) {

    override fun convert(holder: BaseViewHolder, item: CloudConfigListItemView) {
        holder.setText(R.id.tv_app_name, item.name)

        val chipGroup = holder.getView<ChipGroup>(R.id.chipGroup)
        chipGroup.removeAllViewsInLayout()

        item.type?.let {
            val typeChip = (context.layoutInflater.inflate(R.layout.single_chip_layout, chipGroup, false) as Chip).apply {
                setText(it)
                val iconRes = when(it) {
                    R.string.android_app -> R.drawable.ic_android_placeholder
                    R.string.magisk_module -> R.drawable.ic_home_magisk_module
                    R.string.shell -> R.drawable.ic_type_shell
                    R.string.shell_root -> R.drawable.ic_type_shell
                    else -> 0
                }
                setChipIconResource(iconRes)
            }
            chipGroup.addView(typeChip, -1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        item.hubName?.let {
            val hubChip = (context.layoutInflater.inflate(R.layout.single_chip_layout, chipGroup, false) as Chip).apply {
                text = it
                val iconRes = when(it) {
                    "GitHub" -> R.drawable.ic_hub_github
                    "Google Play" -> R.drawable.ic_hub_google_play
                    "酷安" -> R.drawable.ic_hub_coolapk
                    else -> R.drawable.ic_hub_website
                }
                setChipIconResource(iconRes)
            }
            chipGroup.addView(hubChip, -1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }


        AppManager.getSingleApp(uuid = item.uuid)?.run {
            holder.itemView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.material_green_200))
        } ?: run {
            holder.itemView.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        }
    }
}