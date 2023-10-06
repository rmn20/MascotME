//
// Created by woesss on 08.07.2023.
//

#include "eas_player.h"
#include "util/log.h"
#include "eas_util.h"

#define LOG_TAG "MMAPI"
#define NUM_COMBINE_BUFFERS 4

namespace mmapi {
    namespace eas {
        EAS_DLSLIB_HANDLE Player::soundBank{nullptr};

        Player::Player(EAS_DATA_HANDLE easHandle, BaseFile *file, EAS_HANDLE stream, const int64_t duration)
                : BasePlayer(duration), easHandle(easHandle), file(file), media(stream) {
            EAS_SetGlobalDLSLib(easHandle, Player::soundBank);
        }

        Player::~Player() {
            close();
        }

        int32_t Player::createPlayer(const char *locator, Player **pPlayer) {
            if (locator == nullptr) {
                return EAS_ERROR_INVALID_PARAMETER;
            }
            EAS_DATA_HANDLE easHandle;
            EAS_RESULT result = EAS_Init(&easHandle);
            if (result != EAS_SUCCESS) {
                return result;
            }
            if (strcmp(locator, "device://tone") == 0) {
                *pPlayer = new Player(easHandle, nullptr, nullptr, -1);
                return EAS_SUCCESS;
            }
            BaseFile *file = new IOFile(locator, "rb");;
            EAS_HANDLE stream;
            int64_t duration;
            result = openSource(easHandle, file, &stream, &duration);
            if (result != EAS_SUCCESS) {
                EAS_Shutdown(easHandle);
                delete file;
                return result;
            }
            *pPlayer = new Player(easHandle, file, stream, duration);
            return result;
        }

        oboe::Result Player::createAudioStream() {
            oboe::AudioStreamBuilder builder;
            builder.setDirection(oboe::Direction::Output);
            builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
            builder.setSharingMode(oboe::SharingMode::Shared);
            builder.setFormat(oboe::AudioFormat::I16);
            builder.setChannelCount(static_cast<int>(easConfig->numChannels));
            builder.setSampleRate(static_cast<int>(easConfig->sampleRate));
            builder.setCallback(this);
            builder.setFramesPerDataCallback(easConfig->mixBufferSize * NUM_COMBINE_BUFFERS);

            oboe::Result result = builder.openStream(oboeStream);
            if (result != oboe::Result::OK) {
                oboeStream.reset();
                ALOGE("%s: can't open audio stream. %s", __func__, oboe::convertToText(result));
            }
            return result;
        }

        void Player::deallocate() {
            BasePlayer::deallocate();
            if (media != nullptr) {
                EAS_Locate(easHandle, media, 0, EAS_FALSE);
            }
        }

        void Player::close() {
            BasePlayer::close();
            if (media != nullptr) {
                EAS_CloseFile(easHandle, media);
            }
            EAS_Shutdown(easHandle);
            delete file;
        }

        int64_t Player::getMediaTime() {
            long pTime = -1;
            if (media == nullptr) {
                return pTime;
            }
            EAS_RESULT result = EAS_GetLocation(easHandle, media, &pTime);
            if (result != EAS_SUCCESS) {
                ALOGE("%s: EAS_GetLocation return %s", __func__, EAS_GetErrorString(result));
                return pTime;
            }
            return pTime * 1000LL;
        }

        int64_t Player::setMediaTime(int64_t now) {
            int64_t time = BasePlayer::setMediaTime(now);
            if (state != STARTED && media != nullptr) {
                EAS_I32 ms = static_cast<EAS_I32>(timeToSet / 1000LL);
                EAS_RESULT result = EAS_Locate(easHandle, media, ms, EAS_FALSE);
                if (result != EAS_SUCCESS) {
                    ALOGE("%s: EAS_Locate() return %s", __func__, EAS_GetErrorString(result));
                }
                timeToSet = -1;
            }
            return time;
        }

        int32_t Player::setVolume(int32_t level) {
            if (media != nullptr) {
                EAS_SetVolume(easHandle, media, level);
            }
            return BasePlayer::setVolume(level);
        }

        int32_t Player::initSoundBank(const char *sound_bank) {
            EAS_DATA_HANDLE easHandle;
            EAS_RESULT result = EAS_Init(&easHandle);
            if (result != EAS_SUCCESS) {
                return result;
            }

            IOFile file(sound_bank, "rb");
            result = EAS_LoadDLSCollection(easHandle, nullptr, &file.easFile);
            if (result == EAS_SUCCESS) {
                EAS_GetGlobalDLSLib(easHandle, &Player::soundBank);
            }
            EAS_Shutdown(easHandle);
            return result;
        }

        int32_t Player::setDataSource(BaseFile *pFile) {
            EAS_HANDLE stream = nullptr;
            int32_t result = openSource(easHandle, pFile, &stream, &duration);
            if (result != EAS_SUCCESS) {
                return result;
            }
            if (media != nullptr) {
                EAS_CloseFile(easHandle, media);
                delete file;
            }
            media = stream;
            file = pFile;
            return result;
        }

        int32_t Player::openSource(EAS_DATA_HANDLE easHandle,
                                   BaseFile *pFile,
                                   EAS_HANDLE *outStream,
                                   int64_t *outDuration) {
            EAS_HANDLE stream;
            EAS_RESULT result = EAS_OpenFile(easHandle, &pFile->easFile, &stream);
            if (result != EAS_SUCCESS) {
                result = EAS_MMAPIToneControl(easHandle, &pFile->easFile, &stream);
            }
            if (result != EAS_SUCCESS) {
                return result;
            }
            result = EAS_Prepare(easHandle, stream);
            if (result != EAS_SUCCESS) {
                EAS_CloseFile(easHandle, stream);
                return result;
            }
            EAS_I32 type = EAS_FILE_UNKNOWN;
            result = EAS_GetFileType(easHandle, stream, &type);
            if (result != EAS_SUCCESS) {
                EAS_CloseFile(easHandle, stream);
                return result;
            }
            ALOGV("EAS_checkFileType(): %s file recognized", EAS_GetFileTypeString(type));
            if (type == EAS_FILE_UNKNOWN) {
                EAS_CloseFile(easHandle, stream);
                result = EAS_ERROR_FILE_FORMAT;
                return result;
            }
            EAS_I32 length = -1;
            result = EAS_ParseMetaData(easHandle, stream, &length);
            if (result != EAS_SUCCESS) {
                EAS_CloseFile(easHandle, stream);
                return result;
            }
            *outStream = stream;
            *outDuration = length >= 0 ? length * 1000LL : length;
            return EAS_SUCCESS;
        }

        oboe::Result Player::prefetch() {
            if (media == nullptr) {
                return oboe::Result::ErrorInvalidState;
            }
            return BasePlayer::prefetch();
        }

        oboe::DataCallbackResult
        Player::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
            memset(audioData, 0, sizeof(EAS_PCM) * easConfig->numChannels * numFrames);
            EAS_STATE easState;
            EAS_State(easHandle, media, &easState);
            if (easState == EAS_STATE_STOPPED || easState == EAS_STATE_ERROR) {
                int64_t playTime = getMediaTime();
                if (looping == -1 || (--loopCount) > 0) {
                    timeToSet = 0;
                    playerListener->postEvent(RESTART, playTime);
                } else {
                    state = PREFETCHED;
                    playerListener->postEvent(STOP, playTime);
                    return oboe::DataCallbackResult::Stop;
                }
            }

            if (timeToSet != -1) {
                EAS_I32 ms = static_cast<EAS_I32>(timeToSet / 1000LL);
                EAS_RESULT result = EAS_Locate(easHandle, media, ms, EAS_FALSE);
                if (result != EAS_SUCCESS) {
                    ALOGE("%s: EAS_Locate() return %s", __func__, EAS_GetErrorString(result));
                }
                timeToSet = -1;
            }

            auto *p = static_cast<EAS_PCM *>(audioData);
            int numFramesOutput = 0;
            EAS_RESULT result;
            for (int i = 0; i < NUM_COMBINE_BUFFERS; i++) {
                EAS_I32 numRendered;
                result = EAS_Render(easHandle, p, easConfig->mixBufferSize, &numRendered);
                if (result != EAS_SUCCESS) {
                    playerListener->postEvent(ERROR, result);
                    ALOGE("%s: EAS_Render() returned %s, numFramesOutput = %d",
                          __func__,
                          EAS_GetErrorString(result),
                          numFramesOutput);
                    return oboe::DataCallbackResult::Stop; // Stop processing to prevent infinite loops.
                }
                p += numRendered * easConfig->numChannels;
                numFramesOutput += numRendered;
            }
            return oboe::DataCallbackResult::Continue;
        }
    } // namespace eas
} // namespace mmapi
