/*
 * Copyright (C) 2014-2022 The Android Open Source Project
 * Copyright (C) 2015 The Linux Foundation
 * Copyright (C) 2026 RevampedFMRadio Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG "FMLIB_JNI"

#include <jni.h>
#include <utils/Log.h>

#if defined(FM_SOC_MTK)
#include "fmr.h"

static int g_idx = -1;
extern struct fmr_ds fmr_data;

#elif defined(FM_SOC_QCOM)
#include "FmRadioController.h"
#include "FM_Const.h"

static FmRadioController *pFMRadio = NULL;

#else
#error "Either FM_SOC_MTK or FM_SOC_QCOM must be defined"
#endif

#ifndef NELEM
#define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#endif

#ifndef FM_SCAN_CH_SIZE_MAX
#define FM_SCAN_CH_SIZE_MAX 200
#endif

static const char *classPathNameFM = "com/android/fmradio/FmNative";

// ----------------------------------------------------------------------------
// Native implementations
// ----------------------------------------------------------------------------

static jboolean native_openDev(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    int ret = FMR_open_dev(g_idx);
    ALOGD("%s [MTK], ret=%d\n", __func__, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (!pFMRadio) {
        pFMRadio = new FmRadioController();
    }
    if (pFMRadio) {
        ret = pFMRadio->open_dev();
    } else {
        ret = JNI_FALSE;
    }
    ALOGD("%s [QCOM], ret=%d\n", __func__, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jboolean native_closeDev(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    int ret = FMR_close_dev(g_idx);
    ALOGD("%s [MTK], ret=%d\n", __func__, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->close_dev();
        delete pFMRadio;
        pFMRadio = NULL;
    }
    ALOGD("%s [QCOM], ret=%d\n", __func__, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jboolean native_powerUp(JNIEnv *env __unused, jobject thiz __unused, jfloat freq)
{
#if defined(FM_SOC_MTK)
    int tmp_freq = (int)(freq * 10);
    int ret = FMR_pwr_up(g_idx, tmp_freq);
    ALOGD("%s [MTK], freq=%d ret=%d\n", __func__, tmp_freq, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    int tmp_freq = (int)(freq * FREQ_MULT);
    if (!pFMRadio) {
        pFMRadio = new FmRadioController();
    }
    if (pFMRadio) {
        ret = pFMRadio->Pwr_Up(tmp_freq);
    } else {
        ret = JNI_FALSE;
    }
    ALOGD("%s [QCOM], freq=%d ret=%d\n", __func__, tmp_freq, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jboolean native_powerDown(JNIEnv *env __unused, jobject thiz __unused, jint type)
{
#if defined(FM_SOC_MTK)
    int ret = FMR_pwr_down(g_idx, type);
    ALOGD("%s [MTK], type=%d ret=%d\n", __func__, type, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    (void)type;
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Pwr_Down();
        delete pFMRadio;
        pFMRadio = NULL;
    }
    ALOGD("%s [QCOM], ret=%d\n", __func__, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jboolean native_tune(JNIEnv *env __unused, jobject thiz __unused, jfloat freq)
{
#if defined(FM_SOC_MTK)
    int tmp_freq = (int)(freq * 10);
    int ret = FMR_tune(g_idx, tmp_freq);
    ALOGD("%s [MTK], freq=%d ret=%d\n", __func__, tmp_freq, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    int tmp_freq = (int)(freq * FREQ_MULT);
    if (pFMRadio) {
        ret = pFMRadio->TuneChannel(tmp_freq);
    } else {
        ret = JNI_FALSE;
    }
    ALOGD("%s [QCOM], freq=%d ret=%d\n", __func__, tmp_freq, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jfloat native_seek(JNIEnv *env __unused, jobject thiz __unused, jfloat freq, jboolean isUp)
{
#if defined(FM_SOC_MTK)
    int tmp_freq = (int)(freq * 100);
    int ret_freq = tmp_freq;
    FMR_set_mute(g_idx, 1);
    int ret = FMR_seek(g_idx, tmp_freq, (int)isUp, &ret_freq);
    if (ret != 0) {
        ret_freq = tmp_freq;
    }
    return (float)ret_freq / 100.0f;
#elif defined(FM_SOC_QCOM)
    float val = freq;
    if (pFMRadio) {
        pFMRadio->Set_mute(true);
        int ret = pFMRadio->Seek((int)isUp);
        ALOGD("%s [QCOM], isUp=%d ret=%d\n", __func__, (int)isUp, ret);
        if (ret > 0) {
            val = (float)ret / FREQ_MULT;
        }
    }
    return val;
#endif
}

static jshortArray native_autoScan(JNIEnv *env, jobject thiz __unused)
{
    jshortArray scanChlarray = NULL;
    int chl_cnt = FM_SCAN_CH_SIZE_MAX;
    uint16_t ScanTBL[FM_SCAN_CH_SIZE_MAX];

#if defined(FM_SOC_MTK)
    FMR_Pre_Search(g_idx);
    int ret = FMR_scan(g_idx, ScanTBL, &chl_cnt);
    if (ret >= 0 && chl_cnt > 0) {
        scanChlarray = env->NewShortArray(chl_cnt);
        if (scanChlarray != NULL) {
            env->SetShortArrayRegion(scanChlarray, 0, chl_cnt, (const jshort*)&ScanTBL[0]);
        }
    }
    FMR_Restore_Search(g_idx);

    if (fmr_data.scan_stop == fm_true) {
        FMR_tune(g_idx, fmr_data.cur_freq);
        scanChlarray = NULL;
    }
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->ScanList(ScanTBL, &chl_cnt);
    } else {
        ret = -1;
    }
    if (ret >= 0 && chl_cnt > 0) {
        scanChlarray = env->NewShortArray(chl_cnt);
        if (scanChlarray != NULL) {
            env->SetShortArrayRegion(scanChlarray, 0, chl_cnt, (const jshort*)&ScanTBL[0]);
        }
    }
#endif
    return scanChlarray;
}

static jboolean native_stopScan(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    int ret = FMR_stop_scan(g_idx);
    ALOGD("%s [MTK], ret=%d\n", __func__, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Stop_Scan_Seek();
    }
    ALOGD("%s [QCOM], ret=%d\n", __func__, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jint native_setRds(JNIEnv *env __unused, jobject thiz __unused, jboolean rdson)
{
#if defined(FM_SOC_MTK)
    int onoff = (rdson == JNI_TRUE) ? FMR_RDS_ON : FMR_RDS_OFF;
    int ret = FMR_turn_on_off_rds(g_idx, onoff);
    ALOGD("%s [MTK], onoff=%d ret=%d\n", __func__, onoff, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = JNI_FALSE;
    if (pFMRadio) {
        ret = pFMRadio->Turn_On_Off_Rds(rdson ? true : false);
    }
    ALOGD("%s [QCOM], rdson=%d ret=%d\n", __func__, (int)rdson, ret);
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jshort native_readRds(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    uint16_t status = 0;
    int ret = FMR_read_rds_data(g_idx, &status);
    if (ret != 0) {
        status = 0;
    }
    return (jshort)status;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->ReadRDS();
    }
    return (jshort)ret;
#endif
}

static jbyteArray native_getPs(JNIEnv *env, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    uint8_t *ps = NULL;
    int ps_len = 0;
    int ret = FMR_get_ps(g_idx, &ps, &ps_len);
    if (ret != 0 || ps == NULL || ps_len <= 0) {
        return NULL;
    }
    jbyteArray psName = env->NewByteArray(ps_len);
    if (psName != NULL) {
        env->SetByteArrayRegion(psName, 0, ps_len, (const jbyte*)ps);
    }
    return psName;
#elif defined(FM_SOC_QCOM)
    char ps[MAX_PS_LEN];
    int ps_len = 0;
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Get_ps(ps, &ps_len);
    } else {
        ret = -1;
    }
    if (ret != 0 || ps_len <= 0) {
        return NULL;
    }
    jbyteArray psArray = env->NewByteArray(ps_len);
    if (psArray != NULL) {
        env->SetByteArrayRegion(psArray, 0, ps_len, (const jbyte*)ps);
    }
    return psArray;
#endif
}

static jbyteArray native_getLrText(JNIEnv *env, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    uint8_t *rt = NULL;
    int rt_len = 0;
    int ret = FMR_get_rt(g_idx, &rt, &rt_len);
    if (ret != 0 || rt == NULL || rt_len <= 0) {
        return NULL;
    }
    jbyteArray rtText = env->NewByteArray(rt_len);
    if (rtText != NULL) {
        env->SetByteArrayRegion(rtText, 0, rt_len, (const jbyte*)rt);
    }
    return rtText;
#elif defined(FM_SOC_QCOM)
    char rt[MAX_RT_LEN];
    int rt_len = 0;
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Get_rt(rt, &rt_len);
    } else {
        ret = -1;
    }
    if (ret != 0 || rt_len <= 0) {
        return NULL;
    }
    jbyteArray rtArray = env->NewByteArray(rt_len);
    if (rtArray != NULL) {
        env->SetByteArrayRegion(rtArray, 0, rt_len, (const jbyte*)rt);
    }
    return rtArray;
#endif
}

static jshort native_activeAf(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    jshort ret_freq = 0;
    int ret = FMR_active_af(g_idx, (uint16_t*)&ret_freq);
    if (ret != 0) {
        return 0;
    }
    return ret_freq;
#elif defined(FM_SOC_QCOM)
    jshort ret_freq = 0;
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Get_AF_freq((uint16_t*)&ret_freq);
    } else {
        ret = -1;
    }
    if (ret != 0) {
        return 0;
    }
    return ret_freq;
#endif
}

static jint native_setMute(JNIEnv *env __unused, jobject thiz __unused, jboolean mute)
{
#if defined(FM_SOC_MTK)
    int ret = FMR_set_mute(g_idx, (int)mute);
    return ret ? JNI_FALSE : JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Set_mute(mute ? true : false);
    }
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jint native_isRdsSupport(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    int supt = -1;
    FMR_is_rdsrx_support(g_idx, &supt);
    return supt;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->IsRds_support();
    }
    return ret;
#endif
}

static jint native_switchAntenna(JNIEnv *env __unused, jobject thiz __unused, jint antenna)
{
#if defined(FM_SOC_MTK)
    int ana = (antenna == 1) ? FM_SHORT_ANA : FM_LONG_ANA;
    int ret = FMR_ana_switch(g_idx, ana);
    if (ret == -ERR_UNSUPT_SHORTANA) {
        return 2;
    } else if (ret != 0) {
        return 1;
    }
    return 0;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    int ana = (antenna == 1) ? FM_SHORT_ANA : FM_LONG_ANA;
    if (pFMRadio) {
        ret = pFMRadio->Antenna_Switch(ana);
    } else {
        ret = 1;
    }
    return ret;
#endif
}

static jboolean native_setLowPowerMode(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    return JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Set_Power_Mode(false);
    }
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

static jboolean native_setNormalPowerMode(JNIEnv *env __unused, jobject thiz __unused)
{
#if defined(FM_SOC_MTK)
    return JNI_TRUE;
#elif defined(FM_SOC_QCOM)
    int ret = 0;
    if (pFMRadio) {
        ret = pFMRadio->Set_Power_Mode(true);
    }
    return ret ? JNI_FALSE : JNI_TRUE;
#endif
}

// ----------------------------------------------------------------------------
// JNI Method Table & Registration
// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
    {"openDev",             "()Z",   (void*)native_openDev },
    {"closeDev",            "()Z",   (void*)native_closeDev },
    {"powerUp",             "(F)Z",  (void*)native_powerUp },
    {"powerDown",           "(I)Z",  (void*)native_powerDown },
    {"tune",                "(F)Z",  (void*)native_tune },
    {"seek",                "(FZ)F", (void*)native_seek },
    {"autoScan",            "()[S",  (void*)native_autoScan },
    {"stopScan",            "()Z",   (void*)native_stopScan },
    {"setRds",              "(Z)I",  (void*)native_setRds },
    {"readRds",             "()S",   (void*)native_readRds },
    {"getPs",               "()[B",  (void*)native_getPs },
    {"getLrText",           "()[B",  (void*)native_getLrText },
    {"activeAf",            "()S",   (void*)native_activeAf },
    {"setMute",             "(Z)I",  (void*)native_setMute },
    {"isRdsSupport",        "()I",   (void*)native_isRdsSupport },
    {"switchAntenna",       "(I)I",  (void*)native_switchAntenna },
    {"setLowPowerMode",     "()Z",   (void*)native_setLowPowerMode },
    {"setNormalPowerMode",  "()Z",   (void*)native_setNormalPowerMode },
};

static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *methods, int numMethods)
{
    jclass clazz = env->FindClass(className);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, methods, numMethods) < 0) {
        ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved __unused)
{
    JNIEnv *env = NULL;
    ALOGI("FM: loading unified LibfmJni\n");

    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed");
        return JNI_ERR;
    }

    if (registerNativeMethods(env, classPathNameFM, gMethods, NELEM(gMethods)) != JNI_TRUE) {
        ALOGE("ERROR: registerNativeMethods failed");
        return JNI_ERR;
    }

#if defined(FM_SOC_MTK)
    if ((g_idx = FMR_init()) < 0) {
        ALOGE("ERROR: FMR_init failed\n");
        return JNI_ERR;
    }
#endif

    return JNI_VERSION_1_6;
}
