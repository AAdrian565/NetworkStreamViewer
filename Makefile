SHELL := /bin/bash
.DEFAULT_GOAL := help

GRADLEW := ./gradlew
ANDROID_SDK ?= $(shell sed -n 's/^sdk\.dir=//p' local.properties | tail -n 1)
ADB ?= $(ANDROID_SDK)/platform-tools/adb

DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk
RELEASE_DIR := app/build/outputs/apk/release

.PHONY: help devices debug install release clean

help:
	@printf '%s\n' \
		'make install                 Build and install the debug APK through ADB' \
		'make devices                 List connected devices and their serials' \
		'make release                 Build the release APK' \
		'make debug                   Build the debug APK only' \
		'make clean                   Remove Gradle build output' \
		'make install ADB_SERIAL=ID   Select a device when multiple are connected'

debug:
	$(GRADLEW) assembleDebug
	@printf 'Debug APK: %s\n' '$(DEBUG_APK)'

devices:
	@test -x '$(ADB)' || { printf 'ADB not found at %s\n' '$(ADB)' >&2; exit 1; }
	@'$(ADB)' devices -l

install:
	@test -x '$(ADB)' || { printf 'ADB not found at %s\n' '$(ADB)' >&2; exit 1; }
	@serial='$(strip $(ADB_SERIAL))'; \
	if [[ -z "$$serial" ]]; then \
		mapfile -t devices < <('$(ADB)' devices | awk 'NR > 1 && NF >= 2 { print $$1 }'); \
		if (( $${#devices[@]} == 0 )); then \
			printf 'No ADB device found. Connect one with USB debugging enabled.\n' >&2; \
			exit 1; \
		elif (( $${#devices[@]} > 1 )); then \
			printf 'Multiple ADB devices found:\n' >&2; \
			printf '  %s\n' "$${devices[@]}" >&2; \
			printf 'Choose one with: make install ADB_SERIAL=DEVICE_ID\n' >&2; \
			exit 1; \
		fi; \
		serial="$${devices[0]}"; \
	fi; \
	state="$$('$(ADB)' devices | awk -v serial="$$serial" '$$1 == serial { print $$2 }')"; \
	case "$$state" in \
		device) ;; \
		unauthorized) \
			printf 'ADB device %s is unauthorized. Unlock the phone and accept "Allow USB debugging?", then retry.\n' "$$serial" >&2; \
			exit 1 ;; \
		offline) \
			printf 'ADB device %s is offline. Reconnect it or restart ADB, then retry.\n' "$$serial" >&2; \
			exit 1 ;; \
		'') \
			printf 'ADB device %s was not found. Run "make devices" to list available serials.\n' "$$serial" >&2; \
			exit 1 ;; \
		*) \
			printf 'ADB device %s is not ready (state: %s).\n' "$$serial" "$$state" >&2; \
			exit 1 ;; \
	esac; \
	printf 'Installing to ADB device: %s\n' "$$serial"; \
	$(MAKE) --no-print-directory debug; \
	'$(ADB)' -s "$$serial" install -r '$(DEBUG_APK)'

release:
	$(GRADLEW) assembleRelease
	@printf 'Release output:\n'
	@find '$(RELEASE_DIR)' -maxdepth 1 -type f -name '*.apk' -print

clean:
	$(GRADLEW) clean
