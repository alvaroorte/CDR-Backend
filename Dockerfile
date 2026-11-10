# Mismo esquema que ticket-backend (imagen Gradle, TZ La Paz, perfil prod), compilando una sola vez.
FROM gradle:9.4.0-jdk21

WORKDIR /app

ENV TZ=America/La_Paz
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV SPRING_PROFILES_ACTIVE=prod

COPY build.gradle settings.gradle ./
COPY gradle/ /app/gradle/
COPY src/ /app/src/
RUN gradle --no-daemon build

# Variables obligatorias en tiempo de ejecución: DB_URL, DB_USERNAME, DB_PASSWORD,
# APP_JWT_SECRET, APP_CORS_ALLOWED_ORIGINS y (solo primer arranque) APP_ADMIN_INITIAL_PASSWORD.
CMD ["java", "-jar", "build/libs/app-1.0.0.jar"]
