# KT Bank - Çalıştırma Talimatları

## 1. Postgres + Redis'i ayağa kaldır
Docker kuruluysa proje kök dizininde:

```
docker compose up -d
```

Bu, `bankdb` adında bir Postgres (5432) ve bir Redis (6379) container'ı başlatır.
Ayarlar `application.properties` içindekilerle birebir eşleşir (kullanıcı: bank / şifre: bank).

Docker yoksa Postgres ve Redis'i kendi makinende kurup aynı port/kullanıcı bilgileriyle
çalıştırman ya da `application.properties`'i kendi ortamına göre güncellemen gerekir.

## 2. Uygulamayı çalıştır

```
mvn spring-boot:run
```

`admin / admin123` ile giriş yap.

## Yeni eklenen üç sistem nasıl doğrulanır?

- **Veritabanı (Postgres):** Uygulamayı durdurup tekrar başlat, kullanıcıların ve
  bakiyelerin hâlâ orada olduğunu gör (H2 in-memory'de bu veriler uçuyordu).
- **Redis Cache:** `GET /api/admin/users` isteğini art arda çağır; ilk çağrı veritabanına
  gider, sonrakiler (5 dakika TTL boyunca) Redis'ten döner. Redis CLI'da
  `redis-cli KEYS "userList*"` ile cache anahtarını görebilirsin.
- **Redis Lock:** Aynı kullanıcı için aynı anda iki `withdraw` isteği gönder (örn. Postman'da
  paralel iki tab). Redisson kilidi sayesinde ikisi de bakiyeyi doğru sırayla düşürür,
  bakiye negatife düşmez.
- **WebSocket:** İki farklı tarayıcı sekmesinde iki farklı kullanıcıyla giriş yap, biri
  diğerine transfer yapsın - alıcı sekmesinde sayfa yenilenmeden bakiyenin anında
  güncellendiğini göreceksin. Admin sekmesinde de "Tüm İşlemler" tablosunun canlı
  büyüdüğünü göreceksin.
