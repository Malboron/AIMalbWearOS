package com.malbandco.aimalb.presentation.tiles

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import androidx.concurrent.futures.ResolvableFuture
import com.malbandco.aimalb.R
import com.malbandco.aimalb.presentation.MainActivity

/**
 * Служба виджета (Tile) для Wear OS. 
 * v1.4.4: Окончательно выверенная реализация по стандартам 2025 года.
 */
class AiTileService : TileService() {
    
    private val ID_IMAGE_LOGO = "app_logo"

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val future = ResolvableFuture.create<TileBuilders.Tile>()
        val deviceParams = requestParams.deviceConfiguration
        
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("92")
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(
                    layout(this, deviceParams)
                )
            ).build()
            
        future.set(tile)
        return future
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val future = ResolvableFuture.create<ResourceBuilders.Resources>()
        
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion("92")
            .addIdToImageMapping(ID_IMAGE_LOGO, ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(ResourceBuilders.AndroidImageResourceByResId.Builder()
                    .setResourceId(R.drawable.ic_aimalb_logo)
                    .build())
                .build())
            .build()
            
        future.set(resources)
        return future
    }

    /**
     * Создание разметки виджета через системный шаблон PrimaryLayout.
     */
    private fun layout(context: Context, deviceParams: DeviceParametersBuilders.DeviceParameters) =
        PrimaryLayout.Builder(deviceParams)
            .setResponsiveContentInsetEnabled(true)
            .setContent(
                // Официальная материальная кнопка - гарантирует наличие области клика
                Button.Builder(context, clickAction(context))
                    .setIconContent(ID_IMAGE_LOGO)
                    .setButtonColors(ButtonColors(
                        ColorBuilders.argb(0xFF000000.toInt()), // Фон черный
                        ColorBuilders.argb(0xFF00E5FF.toInt())  // Иконка голубая
                    ))
                    .setSize(80f)
                    .build()
            )
            .build()

    /**
     * Официальный способ запуска Activity в Wear OS Tiles.
     */
    private fun clickAction(context: Context) = ModifiersBuilders.Clickable.Builder()
        .setId("launch_main_v144")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build()
                ).build()
        ).build()
}
