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

FROM y2khub/cljdroid@sha256:acbfaa63023f30b48763a8aaaf3a08c60a8bc24bf396667b7ff17ef6ff2da0a4 AS apk
COPY --from=source /src/.build/bin/ /target/
RUN mkdir -p /output_apk && /bin/bash /app/build.sh build-release

FROM scratch AS unsigned
COPY --from=apk /output_apk/app-release-unsigned.apk /app-release-unsigned.apk
COPY --from=apk /output_apk/mapping.txt /mapping.txt

FROM apk AS signed
RUN apt-get update \
    && apt-get install -y --no-install-recommends gpg \
    && rm -rf /var/lib/apt/lists/*
COPY signing/sign.sh signing/release.jks.gpg /signing/
RUN --mount=type=secret,id=KEYSTORE_GPG_PASSPHRASE,required=true \
    --mount=type=secret,id=KEYSTORE_PASSWORD,required=true \
    --mount=type=secret,id=KEY_PASSWORD,required=true \
    --mount=type=tmpfs,target=/tmp \
    sh /signing/sign.sh

FROM scratch AS release
COPY --from=signed /output_apk/app-release.apk /app-release.apk
COPY --from=signed /output_apk/mapping.txt /mapping.txt
