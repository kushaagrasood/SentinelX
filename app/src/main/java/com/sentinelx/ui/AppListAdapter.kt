package com.sentinelx.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sentinelx.R
import com.sentinelx.shared.AppInfo
import com.sentinelx.shared.toRiskColor
import com.sentinelx.shared.toRiskEmoji

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onItemClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAppIcon: ImageView   = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView    = view.findViewById(R.id.tvAppName)
        val tvPackageName: TextView = view.findViewById(R.id.tvPackageName)
        val tvPermCount: TextView  = view.findViewById(R.id.tvPermCount)
        val tvRiskEmoji: TextView  = view.findViewById(R.id.tvRiskEmoji)
        val tvRiskScore: TextView  = view.findViewById(R.id.tvRiskScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.ivAppIcon.setImageDrawable(app.icon)
        holder.tvAppName.text = app.appName
        holder.tvPackageName.text = app.packageName
        holder.tvPermCount.text = if (app.sensitivePermissions.isNotEmpty())
            "${app.sensitivePermissions.size} sensitive permission(s)"
        else "No sensitive permissions"
        holder.tvRiskEmoji.text = app.riskScore.toRiskEmoji()
        holder.tvRiskScore.text = "${app.riskScore}/100"
        holder.tvRiskScore.setTextColor(app.riskScore.toRiskColor())
        holder.itemView.setOnClickListener { onItemClick(app) }
    }

    override fun getItemCount() = apps.size
}