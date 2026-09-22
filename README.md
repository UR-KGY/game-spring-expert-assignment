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

## 4. 월드 생성
worldOperations.duringCreation() 메서드  //추측 상 월드를 만드는 동안 조건을 설정하는 메서드
의 매개변수에는 람다식이 들어간다. (Supplier<T> action) 매개변수는 람다식을 넣는 매개변수

worldRepository 에서 현재 존재하는 월드 수를 가져오고 최대 월드 개수(3개) 를 초과하면 예외를 던지고
아니라면 월드 생성을 준비하게 된다.

```JAVA

    public <T> T duringCreation(Supplier<T> action) {
        worldCreationLock.lock();
        boolean releaseAfterTransaction = false;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        worldCreationLock.unlock();
                    }
                });
                releaseAfterTransaction = true;
            }
            return action.get();
        } finally {
            if (!releaseAfterTransaction) worldCreationLock.unlock();
        }
    }
```




```JAVA

    @Transactional
    public CommittedWorldCreation createWorld(CreateWorldRequest request) {
        if (!baselineReadiness.isReady()) { 
            throw new ServiceUnavailableException("WORLD_BASELINE_INITIALIZING");
        }
        return worldOperations.duringCreation(() -> {
            if (worldRepository.countRootWorlds() >= MAX_WORLDS) { //MAX_WORLD를 기준으로 이미 최대치의 월드가 있다면
                throw new ConflictException("WORLD_LIMIT_REACHED");
            }

            return createPreparedWorld(request);
            // TODO Lv 4: duringCreation() 안에서 기본 월드 3개 제한을 검사하고 createPreparedWorld(request)를 호출합니다
            //  throw new UnsupportedOperationException("Lv 4: 월드 생성을 구현하세요.");
        });

```

## 5. 채팅 저장과 내역 조회 
worldRepository.findById(worldId) 로 요청으로 받은 월드가 존재하는지 여부를 확인

월드가 존재한다면 chatMessageRepository.save()로 요청으로 받은 데이터를 매개변수로 넣어 db에 저장하고
저장한 데이터를 반환 받는다.

이후엔 반환받은 데이터를 가지고 응답 dto 를 만들어 반환

```JAVA
@Transactional
    public ChatMessageResponse saveMessage(Long worldId, String sender, String content) {
        // TODO Lv 5: 채팅을 저장하고 savedResponse(worldId, saved)의 결과를 반환합니다.
        World world = worldRepository.findById(worldId)
                .orElseThrow(() -> new NotFoundException("WORLD_NOT_FOUND"));

        ChatMessage chatMessage = chatMessageRepository.save(new ChatMessage(
                world, sender, content
        ));

        return new ChatMessageResponse(
                chatMessage.getSenderNickname(), chatMessage.getContent(), chatMessage.getCreatedAt()
        );

//        throw new UnsupportedOperationException("Lv 5: 채팅 저장을 구현하세요.");
    }
```

## 6.최근 채팅 조회 api 구현

명세서 적힌대로 구현
@GetMapping("/worlds/{worldId}/chats")
@PathVariable Long worldId 
@RequestParam(defaultValue = "50") int limit  //default 값을 설정하기 위해 RequestParam 으로 기본값 설정


```
//전
@RestController
@RequiredArgsConstructor
public class WorldChatController {

    private final RecentChatQueryService chatService;

    // TODO Lv 6: API 명세에 맞는 요청 매핑과 응답을 구현합니다.
    public ResponseEntity<List<ChatMessageResponse>> chats(Long worldId, int limit) {
        return ResponseEntity.ok(List.of());
    }
}

//후
@RestController
@RequiredArgsConstructor
public class WorldChatController {

    private final RecentChatQueryService chatService;

    @GetMapping("/worlds/{worldId}/chats")
    // TODO Lv 6: API 명세에 맞는 요청 매핑과 응답을 구현합니다.
    public ResponseEntity<List<ChatMessageResponse>> chats(@PathVariable Long worldId,@RequestParam(defaultValue = "50") int limit ) {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.getRecentMessages(worldId,limit));
    }
}
```

7. HandShakeInterceptor 수정

웹소켓을 연결하기 전 데이터를 검증하는 로직을 수정
요청으로 받은 닉네임으로 각각의 db에서 플레이어와 월드를 조회 
만약 등록된 플레이어와 월드가 아니라면 return false를 하는 대신 
return true 로 하되 attributes.put(ATTR_ERROR_CODE, 4001);처럼 attributes 에 오류가 있다는 것을 알려준다.
이렇게 하는 이유는 return false 로 연결조차 실패한다면 무엇이 오류인지 알 수 없기 때문에 
클라이언트 쪽에서 오류를 확인하고 다시 제대로 된 요청을 보내게 하기 위함이다.


```
 // TODO Lv 7: 닉네임으로 플레이어를 조회합니다. 없으면 null을 사용합니다.
        Player player = playerRepository.findByNickname(nickname).orElse(null);
        if (player == null) {
            attributes.put(ATTR_ERROR_CODE, 4000);
            return true;
        }

// TODO Lv 7: worldId로 월드를 조회합니다. 없으면 null을 사용합니다.
        World world = worldRepository.findById(worldId).orElse(null);
        if (world == null || worldRepository.isDimensionChild(worldId)) {
            attributes.put(ATTR_ERROR_CODE, 4001);
            return true;
        }

// TODO Lv 7: nickname과 worldId를 ATTR_NICKNAME, ATTR_WORLD_ID 키로 attributes에 저장합니다.
        attributes.put(ATTR_NICKNAME,nickname);
        attributes.put(ATTR_WORLD_ID,worldId);

        attributes.put(ATTR_PLAYER_ID, player.getId());
        attributes.put(ATTR_WORLD_SEED, (int) world.getSeed());
        attributes.put(ATTR_WORLD_DIFFICULTY, world.getDifficulty());
        return true;

```

