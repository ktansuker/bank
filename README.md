# KT Bank - Çalıştırma Talimatları

## 🐳 Yöntem 1: Her şey Docker'da (önerilen)

Artık uygulamanın kendisi de container'da çalışıyor - tek komutla tüm stack ayağa kalkar:

```
docker compose up -d --build
```

Bu şunları başlatır: `bank-postgres`, `bank-redis`, `bank-rabbitmq`, `bank-pgadmin`, `bank-app`.
İlk çalıştırmada Maven bağımlılıklarını indirdiği için birkaç dakika sürebilir.

Ardından **http://localhost:8080** adresinden `admin / admin123` ile giriş yapabilirsin.

Bu moddan sonra kod değiştirirsen tekrar `docker compose up -d --build` çalıştırman yeterli.

## 💻 Yöntem 2: Sadece altyapı Docker'da, uygulama lokalde (geliştirme için pratik)

```
docker compose up -d postgres redis rabbitmq pgadmin
mvn spring-boot:run
```

Bu durumda uygulama `application.properties`'teki `localhost:5433` (postgres),
`localhost:6379` (redis), `localhost:5673` (rabbitmq) ayarlarını kullanır - Yöntem 1'de
bunlar docker-compose.yml içindeki ortam değişkenleriyle `postgres`/`redis`/`rabbitmq`
servis adlarına otomatik geçiyor, sen bir şey değiştirmene gerek yok.

## ⚠️ Şema değişikliği uyarısı (email alanı eklendi)

Kullanıcılara zorunlu, tekil bir `email` alanı eklendi. Eski veritabanı varsa sıfırla:
```
docker compose down -v
docker compose up -d --build
```

---

## 📜 Logları görme (istediğin özellik burada)

### Uygulama logları + para hareketleri (Docker üzerinden)
```
docker logs -f bank-app
```
Bunun içinde **her para yatırma/çekme/transfer/döviz işlemi** ayrı, okunabilir bir
satır olarak 💰 simgesiyle görünür, örnek:
```
2026-07-16 10:05:12 💰 PARA GÖNDERİLDİ | kullanıcı: kaan | miktar: 40.00 TRY | işlem sonrası bakiye: 80.00 TRY | alıcı: Emirhan
2026-07-16 10:05:12 💰 PARA ALINDI | kullanıcı: Emirhan | miktar: 40.00 TRY | işlem sonrası bakiye: 40.00 TRY | gönderen: kaan
```

Sadece para hareketlerini filtrelemek istersen:
```
docker logs bank-app | grep "💰"
```

Bu loglar ayrıca host makinende kalıcı dosyalar olarak da duruyor (container silinse bile kaybolmaz):
- `./logs/transactions.log` — sadece para hareketleri (yukarıdaki gibi)
- `./logs/application.log` — genel uygulama logları (SQL sorguları dahil)

### PostgreSQL'in kendi sorgu logları (hangi SQL çalıştı)
```
docker logs -f bank-postgres
```
`log_statement=all` ayarı sayesinde veritabanına giden **her** SQL sorgusu (INSERT,
UPDATE, SELECT) burada görünür. Örneğin bir transfer yaptığında `wallets` tablosuna
giden iki `UPDATE` satırını, `transaction_history`'ye giden `INSERT`'leri canlı izleyebilirsin.

### RabbitMQ mesaj akışı (görsel arayüz)
**http://localhost:15673** (kullanıcı: `bank`, parola: `bank`) — Queues sekmesinden
`bank.transactions.notification.queue` ve `bank.transactions.audit.queue`'ya düşen
mesaj sayısını canlı izleyebilirsin.

### Veritabanını görsel arayüzden inceleme
**http://localhost:5050** (email: `admin@bank.com`, parola: `admin123`) — sunucu ekleme
detayları için aşağıya bak.

### Uygulama sağlık durumu (Actuator)
**http://localhost:8080/actuator/health** — uygulamanın, veritabanının, Redis ve
RabbitMQ bağlantısının canlı olup olmadığını JSON olarak gösterir. Profesyonel
sistemlerde bu tür bir endpoint, yük dengeleyicilerin (load balancer) "bu sunucu
trafiğe hazır mı" kontrolü için kullanılır.

---

## pgAdmin'e sunucu ekleme

1. http://localhost:5050 aç, `admin@bank.com` / `admin123` ile gir
2. Add New Server → General → Name: `Bank DB`
3. Connection sekmesi:
   - Host name/address: `postgres` (container adı, `localhost` DEĞİL)
   - Port: `5432` (container'ın iç portu, `5433` değil)
   - Maintenance database: `bankdb`
   - Username / Password: `bank` / `bank`
4. Save → sol menüden `Bank DB → Databases → bankdb → Schemas → public → Tables`

## Yeni: Transfer artık ID / E-posta / Kullanıcı Adı ile yapılabilir

Transfer formundaki "Alıcı" alanına üç formattan biri yazılabilir:
- Sayısal bir değer (`42`) -> kullanıcı ID'si olarak aranır
- `@` içeren bir değer (`kaan@ornek.com`) -> e-posta olarak aranır
- Diğer her şey -> kullanıcı adı olarak aranır
