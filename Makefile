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
	@ rm -rf .build/bin/test .build/test
	@ $(MAKE) build
	@ mkdir -p .build/test
	@ find .build/bin/test/src/main/java -name '*_test.java' | sort > .build/test/sources.list
	@ test -s .build/test/sources.list
	@ javac -d .build/test -sourcepath .build/bin/app/src/main/java:.build/bin/test/src/main/java @.build/test/sources.list
	@ set -e; while IFS= read -r source; do \
		class=$$(printf '%s' "$${source#.build/bin/test/src/main/java/}" | sed 's|/|.|g; s|\.java$$||'); \
		java -cp .build/test "$$class"'$$Runner'; \
		printf 'PASS: %s\n' "$$class"; \
	done < .build/test/sources.list

.PHONY: test-ui
test-ui: build
	@ mkdir -p .build/test-ui
	@ ly2k --target java < test-ui/disabled_history_test.clj > .build/test-ui/disabled_history_test.java
	@ javac -d .build/test-ui .build/bin/app/src/main/java/y2k/language/language_runtime.java .build/test-ui/disabled_history_test.java
	@ java -cp .build/test-ui 'test_ui.disabled_history_test$$Runner'
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
