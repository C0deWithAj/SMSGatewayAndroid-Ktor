package com.mojo.smsserver.data.model.api

import com.google.gson.annotations.SerializedName


data class MufuResponseGson(@SerializedName("MuFU_response") var MuFUResponse: MuFUResponse? = MuFUResponse())

data class MuFUResponse(
    @SerializedName("stat") var stat: String? = null,
    @SerializedName("statMsg") var statMsg: String? = null
)

