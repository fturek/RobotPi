package com.example.robotpi.ui.main

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.example.robotpi.R
import com.example.robotpi.api.RobotService
import kotlinx.android.synthetic.main.main_fragment.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class MainFragment : Fragment(), Callback<Unit>,
    CompoundButton.OnCheckedChangeListener, View.OnTouchListener {

    private lateinit var service: RobotService

    private lateinit var player: SimpleExoPlayer
    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val raspberryLocalHost = "http://192.168.0.108:8000/"

        val retrofit = Retrofit.Builder()
            .baseUrl(raspberryLocalHost)
            .build()

        service = retrofit.create<RobotService>(RobotService::class.java)

        player = SimpleExoPlayer.Builder(context!!).build()


        video_player.player = player

        dataSourceFactory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context!!, "RobotPi")
        )

        button_drive_forward.setOnTouchListener(this)
        button_drive_backwards.setOnTouchListener(this)
        button_drive_left.setOnTouchListener(this)
        button_drive_right.setOnTouchListener(this)

        switch_camera.setOnCheckedChangeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_button_set_localhost -> {
                if (context == null) {
                    return super.onOptionsItemSelected(item)
                }
                MaterialDialog(context!!).show {
                    input()
                    positiveButton(R.string.alert_local_host_set)
                }
                true
            }
            R.id.menu_button_turn_off_device -> {
                service
                    .deviceTurnOff()
                    .enqueue(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                when (v?.id) {
                    R.id.button_drive_forward -> {
                        service
                            .driveForward()
                            .enqueue(this)
                    }

                    R.id.button_drive_backwards -> {
                        service
                            .driveBackwards()
                            .enqueue(this)
                    }

                    R.id.button_drive_left -> {
                        service
                            .driveLeft()
                            .enqueue(this)
                    }

                    R.id.button_drive_right -> {
                        service
                            .driveRight()
                            .enqueue(this)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                service
                    .driveStop()
                    .enqueue(this)
            }
        }

        return true
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (isChecked) {
            false -> {
//                service
//                    .cameraTurnOff()
//                    .enqueue(object : Callback<Unit> {
//                        override fun onFailure(call: Call<Unit>, t: Throwable) {
//                            text_view_camera_info.setText(R.string.switch_camera_off_text)
//                            video_player.stopPlayback()
//                        }
//
//                        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
//                            text_view_camera_info.setText(R.string.switch_camera_off_text)
//                            video_player.stopPlayback()
//                        }
//
//                    })
            }

            true -> {
                val uri = Uri.parse("http://techslides.com/demos/sample-videos/small.mp4")

                // This is the MediaSource representing the media to be played.
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(uri)
                // Prepare the player with the source.
                player.prepare(videoSource)

//                service
//                    .cameraTurnOn()
//                    .enqueue(object : Callback<Unit> {
//                        override fun onFailure(call: Call<Unit>, t: Throwable) {
//                            text_view_camera_info.setText(R.string.switch_camera_off_text)
//                            video_player.stopPlayback()
//                        }
//
//                        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
//                            text_view_camera_info.setText(R.string.switch_camera_on_text)
//
//                        }
//                    })

            }
        }
    }

    override fun onFailure(call: Call<Unit>, t: Throwable) {
        Log.e("RobotPi", "Failure...", t)
    }

    override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
        Log.e("RobotPi", "Success ! ")
    }

}
