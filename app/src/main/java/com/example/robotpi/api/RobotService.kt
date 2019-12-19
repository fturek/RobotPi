package com.example.robotpi.api

import retrofit2.Call
import retrofit2.http.GET

interface RobotService {

    @GET("robocik/prosto")
    fun driveForward(): Call<Unit>

    @GET("robocik/tyl")
    fun driveBackwards(): Call<Unit>

    @GET("robocik/lewo")
    fun driveLeft(): Call<Unit>

    @GET("robocik/prawo")
    fun driveRight(): Call<Unit>

    @GET("robocik/stoj")
    fun driveStop(): Call<Unit>

    @GET("robocik/kameraon")
    fun cameraTurnOn(): Call<Unit>

    @GET("robocik/kameraoff")
    fun cameraTurnOff(): Call<Unit>

    @GET("robocik/wylacz")
    fun deviceTurnOff(): Call<Unit>
}