# --- Aşama 1: Derleme ---
# Maven + JDK içeren bir imajla projeyi derliyoruz, çıktı olarak sadece jar dosyasını alacağız
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Önce sadece pom.xml'i kopyalayıp bağımlılıkları indiriyoruz. Bu sayede sadece kod
# değiştiğinde (pom.xml aynı kaldığında) Docker bu katmanı cache'ten kullanır, her
# seferinde tüm bağımlılıkları yeniden indirmez - build çok daha hızlı olur.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Aşama 2: Çalıştırma ---
# Derleme aşamasındaki ağır Maven/JDK imajını değil, sadece çalıştırma için gereken
# küçük bir JRE imajını kullanıyoruz - son imaj boyutu çok daha küçük olur.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Loglar için ayrı bir klasör - docker-compose.yml'de bunu host makinene mount edeceğiz,
# böylece container silinse bile log dosyaları kalıcı olur.
RUN mkdir -p /app/logs

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
