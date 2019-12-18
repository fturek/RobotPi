package com.example.robotpi.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.example.robotpi.R
import com.example.robotpi.api.RobotService
import com.example.robotpi.sharedprefs.SharedPrefs
import com.example.robotpi.utils.AlertHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class MainFragment : Fragment(), Callback<Unit>,
    CompoundButton.OnCheckedChangeListener, View.OnTouchListener {

    private lateinit var service: RobotService

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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        savedLocalApi = SharedPrefs.readLocalhost(context as Activity) ?: return

        val raspberryLocalHost = "http://$savedLocalApi:8000/"

        val retrofit = Retrofit.Builder()
            .baseUrl(raspberryLocalHost)
            .build()

        service = retrofit.create<RobotService>(RobotService::class.java)

        web_view.settings.javaScriptEnabled = true
        web_view.settings.setSupportZoom(true)
        web_view.settings.builtInZoomControls = true

        progressDialog = AlertHelper.setProgressDialog(context!!, "Loading...")

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
                    input(hint = savedLocalApi) { _, text ->
                        SharedPrefs.saveLocalhost(activity!!, text.toString())
                        savedLocalApi = text.toString()
                    }
                    positiveButton(R.string.dialog_local_host_set)
                    Snackbar
                        .make(
                            activity!!.findViewById(R.id.container),
                            R.string.alert_raspberry_host_set,
                            Snackbar.LENGTH_SHORT
                        )
                        .show()

                }
                true
            }
            R.id.menu_button_turn_off_device -> {
                service
                    .deviceTurnOff()
                    .enqueue(this)

                Snackbar
                    .make(
                        activity!!.findViewById(R.id.container),
                        R.string.alert_raspberry_turned_off,
                        Snackbar.LENGTH_SHORT
                    )
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

        return false
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {

        progressDialog.show()
        when (isChecked) {
            false -> {
                service
                    .cameraTurnOff()
                    .enqueue(object : Callback<Unit> {
                        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                            Log.e("RobotPi", "Turning off camera")
                            web_view.visibility = View.INVISIBLE
                            text_view_camera_info.setText(R.string.switch_camera_off_text)
                            progressDialog.hide()
                            switch_camera.isChecked = false
                        }

                        override fun onFailure(call: Call<Unit>, t: Throwable) {
                            Log.e("RobotPi", "OnFailure, ", t)
                            text_view_camera_info.setText(R.string.switch_camera_off_text)
                            web_view.visibility = View.INVISIBLE
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
                            progressDialog.hide()
                            Log.e("RobotPi", "Turning on camera")

                            val url = "http://$savedLocalApi:8081/"

                            Thread.sleep(1000)
                            web_view.loadUrl(url)

                            switch_camera.isChecked = true

                            text_view_camera_info.setText(R.string.switch_camera_on_text)
                            web_view.visibility = View.VISIBLE
                            progressDialog.hide()
                        }

                        override fun onFailure(call: Call<Unit>, t: Throwable) {
                            Log.e("RobotPi", "OnFailure, ", t)
                            text_view_camera_info.setText(R.string.switch_camera_off_text)
                            web_view.visibility = View.INVISIBLE
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