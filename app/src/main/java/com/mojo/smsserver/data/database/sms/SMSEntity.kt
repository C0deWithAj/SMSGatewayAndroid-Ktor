package com.mojo.smsserver.data.database.sms

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mojo.smsserver.data.model.SMSItem
import com.mojo.smsserver.util.AppConstant


@Entity
data class SMSEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var msg: String,
    var phoneNum: String,
    var deliveryStatus: Int = AppConstant.SMS_STATUS_PENDING,
    var source: String
)

fun SMSItem.asDatabaseModel() = SMSEntity(
    msg = msg, phoneNum = number, source = src, deliveryStatus = deliveryStatus, id = id
)


fun SMSEntity.asUIModel() = SMSItem(
    msg = msg, number = phoneNum, src = source, deliveryStatus = deliveryStatus, id = id
)