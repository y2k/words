FROM y2khub/language AS source
RUN apt-get update \
    && apt-get install -y --no-install-recommends make git ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && ln -s /usr/local/bin/language /usr/local/bin/ly2k
ENV LY2K_PACKAGES_DIR=/packages
RUN git clone --depth 1 https://github.com/y2k/packages.git /packages \
    && git clone --depth 1 https://github.com/y2k/language.git /language \
    && mkdir -p /packages/prelude/1.0.0/js /packages/prelude/1.0.0/java \
    && cp /language/prelude/language_runtime.js /packages/prelude/1.0.0/js/ \
    && cp /language/prelude/language_runtime.java /packages/prelude/1.0.0/java/
WORKDIR /src
COPY Makefile build.clj ./
COPY src/ src/
COPY test/ test/
COPY resources/ resources/
RUN make build .build/bin/app/dependencies.gradle

FROM y2khub/cljdroid@sha256:1fcd736388828a576cbee3f750b20b886bff21b5990c7005ba30ca77fad88112 AS apk
COPY --from=source /src/.build/bin/ /target/
RUN mkdir -p /output_apk && /bin/bash /app/build.sh build

FROM scratch
COPY --from=apk /output_apk/app-debug.apk /app-debug.apk
