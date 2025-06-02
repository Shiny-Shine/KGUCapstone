package com.example.kgucapstone.model
import com.google.firebase.Timestamp

data class Medicine(
    val name: String = "", // 약명
    val remedy: List<String> = emptyList(), // 효능
    val caution: List<Banner> = emptyList(), // 주의사항
    val doseMethod: List<Banner> = emptyList(), // 복용방법
    val doseDay: List<String> = emptyList(), // 복용요일
    val doseTime: WhatTime = WhatTime(8,0,true),// 복용시간
    val isAlertSound: Boolean = false, // 소리 알람 여부
    val isAlertVibe: Boolean = false, //진동 울림 여부
    val doseOnceAmount: Int = 0, // 1회 복용량
    val isNowDose: Boolean = false, // 현재 복용 여부
    val doseStartDay: Timestamp, //복약 시작일
    val doseEndDay: Timestamp = Timestamp.now(), // 복약 종료일
)

data class Banner(
    val imageURL: String = "",
    val imageExplain: String = "",
)

data class WhatTime(
    val hour: Int = 0,         // 1~12
    val minute: Int = 0,       // 0~59
    val isAm: Boolean = true   // true: 오전, false: 오후
)
