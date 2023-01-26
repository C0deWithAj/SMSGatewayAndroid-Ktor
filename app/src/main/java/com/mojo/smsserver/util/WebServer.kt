package com.mojo.smsserver.util

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException


class WebServer : NanoHTTPD(8080) {
    override fun serve(
        uri: String,
        method: Method,
        header: Map<String, String>,
        parameters: Map<String, String>,
        files: Map<String, String>
    ): Response {
        Log.i("Test321", "Serve called")
        var answer: String? = "<html><body><h1>Hello AJ</h1>\n";
        try {
//            for (set in parameters) {
//                Log.i("Test321", "PARAMS = ${set.value}")
//            }
//
//            for (set in files) {
//                Log.i("Test321", "FILES = ${set.value}")
//            }

            Log.i("Test321", "URI = ${uri}")
        } catch (ioe: IOException) {
            Log.w("Httpd", ioe.toString())
        }
        return newFixedLengthResponse(answer);
    }
}