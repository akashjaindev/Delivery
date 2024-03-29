package app.delivery.di.module

import android.content.Context
import app.delivery.BuildConfig
import app.delivery.model.ThreadModel
import app.delivery.repository.database.DbRepository
import app.delivery.repository.network.ApiInterface
import app.delivery.repository.network.NetworkRepository
import app.delivery.utils.NetworkConnectionUtil
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


/**
 * provides instance of objects used for performing network operations
 */
@Module
class NetworkModule {

    @Provides
    @Singleton
    fun providesOkHttpClient(thread: ThreadModel): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .method(original.method(), original.body())
//                            .addHeader("Authorization", mAuth)
                    .build()
                chain.proceed(request)
            }.dispatcher(Dispatcher(thread.networkThread))

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(logging)
        }
        return okHttpClientBuilder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofitInterface(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun providePostApi(retrofit: Retrofit): ApiInterface {
        return retrofit.create(ApiInterface::class.java)
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        dbRepo: DbRepository,
        context: Context,
        connection: ApiInterface,
        networkConnectionUtil: NetworkConnectionUtil
    ): NetworkRepository {
        return NetworkRepository(dbRepo, context, connection, networkConnectionUtil)
    }
}