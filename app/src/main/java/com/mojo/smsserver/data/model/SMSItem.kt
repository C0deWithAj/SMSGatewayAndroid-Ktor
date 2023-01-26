package com.mojo.smsserver.data.model

data class SMSItem(var msg: String, var number: String, var src: String, var deliveryStatus: Int, var id: Int)