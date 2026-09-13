ANDROID_DEPENDENCIES := .build/bin/app/dependencies.gradle
.DEFAULT_GOAL := build
.DELETE_ON_ERROR:

$(ANDROID_DEPENDENCIES): resources/android.clj
	@ mkdir -p $(dir $@)
	@ ly2k --target eval < $< > $@

.PHONY: build
build:
	@ mkdir -p .build
	@ cat build.clj | ly2k --target eval | $(MAKE) -f -
	@ mkdir -p .build/bin/app/src/main/java/y2k/language
	@ cp $$LY2K_PACKAGES_DIR/prelude/1.0.0/java/language_runtime.java .build/bin/app/src/main/java/y2k/language
	@ cat resources/manifest.clj | ly2k --target eval > .build/bin/app/src/main/AndroidManifest.xml
	@ mkdir -p .build/bin/app/src/main/res
	@ cp -R resources/res/. .build/bin/app/src/main/res/

.PHONY: test
test:
	@ { cat src/words/effect.clj src/words/app.clj test/words/main_test.clj; printf '\n(words.main-test/test)\n'; } | ly2k --target eval

.PHONY: check-ui
check-ui: build
	@ mkdir -p .build/check-ui
	@ ly2k --target java < checks/disabled_history.clj > .build/check-ui/disabled_history.java
	@ javac -d .build/check-ui .build/bin/app/src/main/java/y2k/language/language_runtime.java .build/check-ui/disabled_history.java
	@ java -cp .build/check-ui 'checks.disabled_history$$Runner'
	@ printf '%s\n' 'PASS: old answers disabled, old taps ignored, new question active'

.PHONY: run
run: build_apk
	@ adb install -r .build/apk/app-debug.apk
	@ adb shell am start -n 'im.y2k.fixme/words.main\$$MainActivity'

.PHONY: build_apk
build_apk: build $(ANDROID_DEPENDENCIES)
	@ mkdir -p .build/apk && rm -f .build/apk/*.apk
	@ docker run --rm \
		-v "$$PWD/.build/cache:/root/.gradle" \
		-v "$$PWD/.build/bin:/target" \
		-v "$$PWD/.build/apk:/output_apk" \
		y2khub/cljdroid build

.PHONY: clean
clean:
	@ rm -rf .build/bin
	@ rm -rf .build/apk
