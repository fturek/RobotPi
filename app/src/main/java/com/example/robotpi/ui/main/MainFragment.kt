package com.example.robotpi.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.example.robotpi.R
import com.example.robotpi.api.RobotService
import com.example.robotpi.sharedprefs.SharedPrefs
import com.example.robotpi.utils.AlertHelper
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.main_fragment.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class MainFragment : Fragment(), Callback<Unit>,
    CompoundButton.OnCheckedChangeListener, View.OnTouchListener {

    private lateinit var service: RobotService

    private lateinit var player: SimpleExoPlayer
    private lateinit var dataSourceFactory: DefaultDataSourceFactory
    private lateinit var savedLocalApi: String
    private lateinit var progressDialog: AlertDialog

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        savedLocalApi = SharedPrefs.readLocalhost(context as Activity) ?: return

        val raspberryLocalHost = "http://$savedLocalApi:8000/"

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

        progressDialog = AlertHelper.setProgressDialog(context!!, "Loading")

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

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.menu_ip)
        item.title = savedLocalApi

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_button_set_localhost -> {
                if (context == null) {
                    return super.onOptionsItemSelected(item)
                }
                MaterialDialog(context!!).show {
                    val savedLocalApi = SharedPrefs.readLocalhost(activity!!)
                    input(hint = savedLocalApi) { _, text ->
                        SharedPrefs.saveLocalhost(activity!!, text.toString())
                    }
                    positiveButton(R.string.dialog_local_host_set)
                }
                true
            }
            R.id.menu_button_turn_off_device -> {
                service
                    .deviceTurnOff()
                    .enqueue(this)

                Snackbar
                    .make(activity!!.findViewById(R.id.container), R.string.alert_raspberry_turned_off, Snackbar.LENGTH_SHORT)
                    .show()

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
        progressDialog.show()

        when (isChecked) {
            false -> {
                service
                    .cameraTurnOff()
                    .enqueue(object : Callback<Unit> {
                        override fun onFailure(call: Call<Unit>, t: Throwable) {
                            text_view_camera_info.setText(R.string.switch_camera_off_text)
                            player.stop()
                            progressDialog.hide()
                            switch_camera.isChecked = false
                        }

                        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                            text_view_camera_info.setText(R.string.switch_camera_off_text)
                            player.stop()
                            progressDialog.hide()
                            switch_camera.isChecked = false
                        }

                    })
            }

            true -> {
                service
                    .cameraTurnOn()
                    .enqueue(object : Callback<Unit> {
                        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                            text_view_camera_info.setText(R.string.switch_camera_on_text)
                            val uri =
                                Uri.parse("http://techslides.com/demos/sample-videos/small.mp4")

                            // This is the MediaSource representing the media to be played.
                            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(uri)
                            // Prepare the player with the source.
                            player.prepare(videoSource)
                            progressDialog.hide()
                            switch_camera.isChecked = true
                        }

                        override fun onFailure(call: Call<Unit>, t: Throwable) {
                            text_view_camera_info.setText(R.string.switch_camera_off_text)
                            player.stop()
                            progressDialog.hide()
                            switch_camera.isChecked = false
                        }
                    })
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
