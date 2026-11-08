package io.lemoncore.opendatadex

import android.app.Application
import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade

class OpenDataDexApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { container.http }))
            // Sun/Moon and Let's Go sprites are animated GIFs; Sword/Shield onwards are animated WebP.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
        }
        .diskCache {
            // Sprites are small and immutable; a generous cache makes the dex usable offline after one browse.
            DiskCache.Builder().directory(context.cacheDir.resolve("images")).maxSizeBytes(256L * 1024 * 1024).build()
        }
        .crossfade(true)
        .build()
}

val Context.container: AppContainer get() = (applicationContext as OpenDataDexApp).container
