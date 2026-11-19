package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ErApiResponse(
  val result: String? = null,
  @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long? = null,
  @Json(name = "base_code") val baseCode: String? = null,
  val rates: Map<String, Double>? = null,
)

@JsonClass(generateAdapter = true)
data class GoldApiSpotResponse(
  val name: String? = null,
  val price: Double? = null,
  val symbol: String? = null,
  val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class GoldPriceOrgResponse(
  val items: List<GoldPriceOrgItem>? = null,
)

@JsonClass(generateAdapter = true)
data class GoldPriceOrgItem(
  val curr: String? = null,
  val xauPrice: Double? = null,
  val xagPrice: Double? = null,
  val pcXau: Double? = null,
  val pcXag: Double? = null,
)

@JsonClass(generateAdapter = true)
data class FrankfurterSeriesResponse(
  val amount: Double? = null,
  val base: String? = null,
  val rates: Map<String, Map<String, Double>>? = null,
)

interface MarketApiService {
  @GET("https://open.er-api.com/v6/latest/USD")
  suspend fun getErApiLatestRates(): ErApiResponse

  @GET("https://api.gold-api.com/price/{symbol}")
  suspend fun getGoldApiSpot(@Path("symbol") symbol: String): GoldApiSpotResponse

  @GET("https://data-asg.goldprice.org/dbXRates/USD")
  suspend fun getGoldPriceOrgRates(): GoldPriceOrgResponse

  @GET("https://api.frankfurter.app/{range}")
  suspend fun getFrankfurterSeries(
    @Path("range") range: String,
    @Query("from") from: String,
    @Query("to") to: String,
  ): FrankfurterSeriesResponse

  @GET
  suspend fun getRawJson(@Url url: String): ResponseBody
}

object NetworkModule {
  val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(6, TimeUnit.SECONDS)
      .readTimeout(8, TimeUnit.SECONDS)
      .callTimeout(10, TimeUnit.SECONDS)
      .build()
  }

  val apiService: MarketApiService by lazy {
    Retrofit.Builder()
      .baseUrl("https://open.er-api.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create())
      .build()
      .create(MarketApiService::class.java)
  }
}
