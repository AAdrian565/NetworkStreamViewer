SHELL := /bin/bash
.DEFAULT_GOAL := help

GRADLEW := ./gradlew
ANDROID_SDK ?= $(shell sed -n 's/^sdk\.dir=//p' local.properties | tail -n 1)
ADB ?= $(ANDROID_SDK)/platform-tools/adb
IMAGE_MAGICK ?= convert

DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk
APP_COMPONENT := com.adriant.networkstreamviewer/.MainActivity
RELEASE_DIR := app/build/outputs/apk/release
ICON_ARTWORK := app/src/main/res/drawable-nodpi/ic_launcher_artwork.png

.PHONY: help devices debug install release icon clean

help:
	@printf '%s\n' \
		'make install                 Build, install, and open the debug app through ADB' \
		'make devices                 List connected devices and their serials' \
		'make release                 Build the release APK' \
		'make debug                   Build the debug APK only' \
		'make icon                    Generate launcher icons from Icon.png' \
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
	$(MAKE) --no-print-directory debug && \
	'$(ADB)' -s "$$serial" install -r '$(DEBUG_APK)' && \
	'$(ADB)' -s "$$serial" shell am start -n '$(APP_COMPONENT)'

release:
	$(GRADLEW) assembleRelease
	@printf 'Release output:\n'
	@find '$(RELEASE_DIR)' -maxdepth 1 -type f -name '*.apk' -print

icon:
	@command -v '$(IMAGE_MAGICK)' >/dev/null || { \
		printf 'ImageMagick command not found: %s\n' '$(IMAGE_MAGICK)' >&2; \
		exit 1; \
	}
	@test -f 'Icon.png' || { \
		printf 'Icon source not found: Icon.png\n' >&2; \
		exit 1; \
	}
	@mkdir -p '$(dir $(ICON_ARTWORK))'
	@'$(IMAGE_MAGICK)' 'Icon.png' \
		-filter Lanczos -resize '260x260!' \
		-background none -gravity center -extent '432x432' -strip \
		'$(ICON_ARTWORK)'
	@for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do \
		density="$${spec%%:*}"; \
		size="$${spec##*:}"; \
		edge="$$((size - 1))"; \
		radius="$$((size * 11 / 50))"; \
		center="$$((size / 2))"; \
		directory="app/src/main/res/mipmap-$$density"; \
		mkdir -p "$$directory"; \
		'$(IMAGE_MAGICK)' 'Icon.png' \
			-filter Lanczos -resize "$${size}x$${size}!" \
			\( +clone -alpha transparent -fill white \
			-draw "roundrectangle 0,0,$$edge,$$edge,$$radius,$$radius" \) \
			-compose DstIn -composite -compose Over -strip -quality 92 \
			"$$directory/ic_launcher.webp"; \
		'$(IMAGE_MAGICK)' 'Icon.png' \
			-filter Lanczos -resize "$${size}x$${size}!" \
			\( +clone -alpha transparent -fill white \
			-draw "circle $$center,$$center $$center,0" \) \
			-compose DstIn -composite -compose Over -strip -quality 92 \
			"$$directory/ic_launcher_round.webp"; \
	done
	@printf 'Generated Android launcher icons from Icon.png\n'

clean:
	$(GRADLEW) clean
