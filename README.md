# Web Craft 개인 프로젝트 


## 1. 프로젝트와 Docker 의 DB 연결
application.properties 내에 환경변수 설정
```properties
# jpa 설정
spring.datasource.url=jdbc:mysql://localhost:3306/webcraft
spring.datasource.username=root
spring.datasource.password=12345678
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

#Redis 설정
spring.data.redis.host=localhost
spring.data.redis.port=6379
```
## 2. 인덱스 설정
인덱스는 특정 컬럼을 기준으로 미리 정렬하여 저장해두는 방식이다. 
인덱스를 기준으로 조회하면 전체를 탐색할 필요없이 빠르게 조회가 가능하다.

```SQL
CREATE INDEX idx_chat_world_created_at ON chat_messages(world_id, created_at);
```

```Plaintext
[원본 테이블 chat] — 그대로, 정렬 안 됨
┌────┬──────────┬────────────┬─────────┐
│ id │ world_id │ created_at │ message │
├────┼──────────┼────────────┼─────────┤
│ 1  │    3     │   10:00    │  ...    │
│ 2  │    1     │   09:00    │  ...    │
│ 3  │    2     │   08:30    │  ...    │
│ 4  │    1     │   09:10    │  ...    │
└────┴──────────┴────────────┴─────────┘

[별도로 저장되는 인덱스 구조물] — 여기만 정렬됨
┌──────────┬────────────┬──────────────────┐
│ world_id │ created_at │ 원본 위치(포인터)  │
├──────────┼────────────┼──────────────────┤
│    1     │   09:00    │  → id=2 위치       │
│    1     │   09:10    │  → id=4 위치       │
│    2     │   08:30    │  → id=3 위치       │
│    3     │   10:00    │  → id=1 위치       │
└──────────┴────────────┴──────────────────┘

```


```java

//변경전

@Getter
@Entity
// TODO Lv 2: 제공된 SQL과 같은 인덱스를 선언합니다.
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "world_id", nullable = false)
    private World world;

   
}

//변경 후

@Getter
@Entity
// TODO Lv 2: 제공된 SQL과 같은 인덱스를 선언합니다.
@Table(name = "chat_messages",
indexes = @Index(name = "idx_chat_world_created_at",columnList = "world_id,created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "world_id", nullable = false)
    private World world;

    
}

```

## 3. 요청 검증과 DTO
@Validated 어노테이션을 통해 요청을 받았을 때 검수하는 것을 활용하는 단계였다. 


1. 요청 DTO 부분

@NotBlank 는 받은 문자열 내의 공백을 허용하지 않는다.
@Size 는 () 안에 min , max 로 문자열의 최소, 최대 크기를 조절한다.
@Pattern 은 정규표현식인데 영문 대소문자, 숫자와 밑줄을 허용하는 검증을 적용한다고 한다.


```java

//전
@Getter
public class CreatePlayerRequest {

    // TODO Lv 3: 2~12글자의 영문 대소문자, 숫자와 밑줄을 허용하는 검증을 적용합니다.
    private final String nickname;

    public CreatePlayerRequest(String nickname) {
        this.nickname = nickname;
    }
}
//후
@Getter
@Validated
public class CreatePlayerRequest {

    // TODO Lv 3: 2~12글자의 영문 대소문자, 숫자와 밑줄을 허용하는 검증을 적용합니다.
    @NotBlank
    @Size(min = 2,max = 12)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private final String nickname;

    public CreatePlayerRequest(String nickname) {
        this.nickname = nickname;
    }
}

```

2. 컨트롤러 부분
명세서에 맞게
닉네임을 '등록' 하는 것이라 @PostMapping
DTO 에서 설정한 검증을 진행하기 위해 매개변수 쪽에 @Valid
객체인 DTO로 요청 데이터를 받는 것이라 @RequestBody
서비스의 메서드와 연결  playerService.createPlayer(request);
반환타입이 void 이기에 return ResponseEntity.status(HttpStatus.CREATED).build();
   

```
//전
@RestController
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    // TODO Lv 3: API 명세에 맞게 요청을 매핑하고, 검증한 요청으로 등록 서비스를 호출한 뒤 성공 응답을 반환합니다.
    public ResponseEntity<Void> create(CreatePlayerRequest request) {
        throw new UnsupportedOperationException("Lv 3: 플레이어 등록 API를 구현하세요.");
    }
}

//후
@RestController
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    // TODO Lv 3: API 명세에 맞게 요청을 매핑하고, 검증한 요청으로 등록 서비스를 호출한 뒤 성공 응답을 반환합니다.
    @PostMapping("players")
    public ResponseEntity<Void> create(@Valid @RequestBody CreatePlayerRequest request) {
        playerService.createPlayer(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

```

3. 서비스 부분
서비스에선 연결된 레포지토리에서 요청으로 받은 닉네임이 존재하는 지 확인하고 있다면 예외로 던지고
없다면 새로 등록한다.

```
//전
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Transactional
    public void createPlayer(CreatePlayerRequest request) {
        // TODO Lv 3: 닉네임 중복을 확인하고 플레이어를 저장합니다.
        throw new UnsupportedOperationException("Lv 3: 플레이어 등록을 구현하세요.");
    }

    private void savePlayer(Player player) {
        try {
            playerRepository.saveAndFlush(player);
        } catch (org.springframework.dao.DataIntegrityViolationException failure) {
            throw new ConflictException("DUPLICATE_NICKNAME");
        }
    }
}

//후
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Transactional
    public void createPlayer(CreatePlayerRequest request) {
        // TODO Lv 3: 닉네임 중복을 확인하고 플레이어를 저장합니다.
        boolean isPlayerEmpty = playerRepository.findByNickname(request.getNickname())
                .isEmpty();

        if(!isPlayerEmpty){
            throw new ConflictException("DUPLICATE_NICKNAME");
        }

        savePlayer(new Player(request.getNickname()));
//        throw new UnsupportedOperationException("Lv 3: 플레이어 등록을 구현하세요.");
    }

    private void savePlayer(Player player) {
        try {
            playerRepository.saveAndFlush(player);
        } catch (org.springframework.dao.DataIntegrityViolationException failure) {
            throw new ConflictException("DUPLICATE_NICKNAME");
        }
    }
}

```
