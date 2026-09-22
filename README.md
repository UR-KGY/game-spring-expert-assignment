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

## 8. webSocketconfig 에 HandShakeInterceptor 등록
.addInterceptors(nicknameInterceptor) 
webSocketConfig에 인터셉터를 등록하였다

```JAVA

//전
@Configuration
@EnableWebSocket
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final NicknameHandshakeInterceptor nicknameInterceptor;
    private final EngineProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // TODO Lv 8: 제공된 인터셉터를 핸들러 등록에 연결합니다.
        registry.addHandler(gameWebSocketHandler, "/ws/worlds/{worldId}")
                .setAllowedOriginPatterns(properties.wsAllowedOrigins().toArray(String[]::new));
    }
}


//후
@Configuration
@EnableWebSocket
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final NicknameHandshakeInterceptor nicknameInterceptor;
    private final EngineProperties properties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // TODO Lv 8: 제공된 인터셉터를 핸들러 등록에 연결합니다.
        registry.addHandler(gameWebSocketHandler, "/ws/worlds/{worldId}")
                .addInterceptors(nicknameInterceptor)
                .setAllowedOriginPatterns(properties.wsAllowedOrigins().toArray(String[]::new));
    }
}

```

## 9. 월드 별 세션 관리
레지스트리는 핸드쉐이크로 연결된 WebSocketSession 들을 모아 저장해놓고 이후 broadcast 와 Handler(웹소켓 진입 시점의 그 핸들러 아님) 에게 
제공하는 클래스이다. 
여기선 worlds 라는 저장소에 각 월드 별로 닉네임과 닉네임에 해당하는 websocketsession 들을 모아놓고 있다.

//저장소
ConcurrentHashMap<Long, ConcurrentHashMap<String, Entry>> worlds : 각 `월드의 id`를 `key`로 하고 `value` 는 `ConcurrentHashMap<String, Entry>`
ConcurrentHashMap<String, Entry> : `key` 는 `플레이어 닉네임` , `value`는 생성자가 `WebsocketSession` 인 `Entry` 인 자료구조이다.

//Registry 는 플레이어 닉네임을 키, 플레이어와 서버의 연결인 WebSocketSession을 Value 로 하여 특정 id의 월드 내 세션저장소에 등록을 시도하는 메서드이다.
//get은 해당 id 의 월드에서 닉네임이 키인 세션을 반환하는 메서드이다.

```JAVA
//새로운 websocketsession 을 등록하는 메서드
public Entry register(Long worldId, String nickname, WebSocketSession session) {
        String nicknameKey = key(nickname);  //키
        Entry candidate = new Entry(session); //값
        AtomicReference<Entry> registered = new AtomicReference<>();
        worlds.compute(worldId, (ignored, current) -> { //worlds 에 worldId 라는 키가 있다면 값인 current 을 매개변수로 하는 람다를 실행 
            ConcurrentHashMap<String, Entry> sessions = current == null  // current가 null이라면 새로운 객체로 생성, 아니라면 기존 current
                    ? new ConcurrentHashMap<>() : current;               // 를  sessions 라는 이름으로 사용한다.
            // TODO Lv 9: putIfAbsent()로 candidate를 등록하고, 새로 등록했으면 added를 true로 설정합니다.
            boolean added = false; 

            //putIfAbsent는 해당 키가 없다면 키와 값을 저장하고 null 반환 아니라면 값을 반환
            Entry addResult = sessions.putIfAbsent(nicknameKey,candidate);  //해당 월드의 저장소에 요청과 동일한 키 값이 있으면 그대로 반환  
                                                                            //아니라면 저장소에 저장 후 null 반환
            if(addResult == null ){ //새로운 세션을 추가하였다면
                added = true;
            }

            if (added) {
                registered.set(candidate);   //세션을 넣음
            }
            return sessions; 
        });
        return registered.get(); 
    }


//해당 월드의 키가 닉네임인 세션을 반환하는 메서드
public Entry get(Long worldId, String nickname) {
        ConcurrentHashMap<String, Entry> sessions = worlds.get(worldId);
        if (sessions == null) {
            return null;
        }
        // TODO Lv 9: sessions에서 key(nickname)에 해당하는 연결을 반환합니다.
        return sessions.get(key(nickname));
    }

```

## 10. Redis 접속 상태 관리 

 ```
public void join(Long worldId, String connectionId) {
        String key = key(worldId);
        // TODO Lv 10: ZSet에 connectionId를 member로, expiresAt()을 score로 저장합니다.
        redisTemplate.opsForZSet().add(key,connectionId,expiresAt());
        redisTemplate.expire(key, KEY_TTL);
    }

    public void leave(Long worldId, String connectionId) {
        // TODO Lv 10: key(worldId)의 ZSet에서 connectionId를 제거합니다.
        String key = key(worldId);
        redisTemplate.opsForZSet().remove(key,connectionId);
    }


```


## 11. 메세지 라우팅과 Ping/Pong
Websocket에서 라우터는 웹소켓의 요청이 들어오면 type 필드 안의 문자열을 근거로 알맞은 핸들러에 요청을 전달하거나 
브로드캐스터에 요청에 에러가 발생했음을 직접알리고 있다.


```
public void route(WsMessageContext context, String payload) {
        final JsonNode message;
        try {
            message = objectMapper.readTree(payload);
        } catch (Exception exception) {
            log.debug("잘못된 JSON 수신: world={}, nickname={}",
                    context.worldId(), context.nickname(), exception);
            error(context, "INVALID_JSON");
            return;
        }
        if (message == null || !message.isObject()) {
            error(context, "INVALID_MESSAGE");
            return;
        }
        JsonNode typeNode = message.get("type"); //타입 필드의 내용을 JsonNode 형태로 가져옴
        String type = typeNode != null && typeNode.isString() ? typeNode.asString() : null; //JsonNode를 문자열로 파싱
        EngineMessageHandler handler = findHandler(type); //타입 필드의 내용을 바탕으로 해당 내용과 일치하는 핸들러 찾기
        if (handler == null) { //일치하는 핸들러가 없다면 브로드캐스트로 요청한 플레이어에게 오류가 발생함을 알려주고 메서드 종료
            error(context, "UNKNOWN_TYPE");
            return;
        }
        try {
            // TODO Lv 11: handler에 context와 message를 전달해 handle()을 호출합니다.
            handler.handle(context,message);  //일치하는 핸들러에 요청과 메세지 전달
        } catch (ActionQueueOverflowException exception) {
            log.warn("액션 큐 상한 초과로 거부: type={}, world={}, nickname={}",
                    type, context.worldId(), context.nickname());
            error(context, "QUEUE_FULL");
        } catch (IllegalArgumentException exception) {
            log.debug("잘못된 메시지 거부: type={}, world={}, nickname={}",
                    type, context.worldId(), context.nickname(), exception);
            error(context, "INVALID_MESSAGE");
        } catch (Exception exception) {
            log.error("메시지 처리 실패: type={}, world={}, nickname={}",
                    type, context.worldId(), context.nickname(), exception);
            error(context, "INTERNAL_ERROR");
        }
    }

```



## 12. 플레이어 이동처리
요청의 type이 move 인 경우 이 핸들러에서 메세지대로 이동을 수행한다.


```
private final WorldEngineManager engineManager;

    @Override
    public String type() {
        return "move";
    }

@Override
    public void handle(WsMessageContext context, JsonNode message) {
        String finalSceneActionId = WsFields.optionalFinalSceneActionId(message);
        // TODO Lv 12: 명세의 이동 값을 읽어 현재 사용자의 이동 요청을 엔진에 전달합니다.
        double x = WsFields.finiteNumber(message, "x");
        double y = WsFields.finiteNumber(message, "y");
        double z = WsFields.finiteNumber(message, "z");
        float yaw = WsFields.finiteFloat(message, "yaw");
        float pitch = WsFields.finiteFloat(message, "pitch");
        boolean crouching = WsFields.booleanValue(message, "crouching");
        boolean gliding = WsFields.booleanValue(message, "gliding");

        PlayerAction.Move move = new PlayerAction.Move(context.nickname(),x,y,z,yaw,pitch,crouching,gliding,finalSceneActionId);
        engineManager.enqueue(context.worldId(),move);
    }

```
 
## 13. 채팅 요청처리
type 이 chat 인 요청을 처리하는 핸들러와 응답  dto의 필드를 채운다.
채팅 메세지는 chatService의 메서드로 db에 저장되고 저장에 성공하였다면 응답dto로 반환된다.

```
@Getter
public class ChatResponse {
    // TODO Lv 13: API 명세에 맞게 응답 필드와 생성자를 완성합니다.
    private final String type = "chat";
    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;
    public ChatResponse(String sender, String content, LocalDateTime timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }
}



@Component
@RequiredArgsConstructor
public class ChatWsHandler implements WsMessageHandler {

    private final ChatService chatService;
    private final ChatDelivery delivery;
    private final ChatRateLimitService rateLimit;
    private final WorldBroadcaster broadcaster;
    private final ChatCommands commands;

    @Override
    public String type() {
        return "chat";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        String content = readContent(message);

        if (content.isBlank() || content.length() > 200) {
            throw new IllegalArgumentException("채팅은 1~200자로 입력해 주세요.");
        }

        Long playerId = (Long) context.session().getAttributes()
                .get(NicknameHandshakeInterceptor.ATTR_PLAYER_ID);
        if (!rateLimit.allow(playerId)) {
            broadcaster.sendTo(context.session(), new Error("CHAT_COOLDOWN"));
            return;
        }

        content = commands.resolve(context.worldId(), context.nickname(), content);

        ChatResponse response = createResponse(context, content);
        delivery.send(context.worldId(), response);
    }

    private String readContent(JsonNode message) {
        // TODO Lv 13: API 명세의 채팅 내용을 읽습니다.
        String text = WsFields.text(message,"content");
        return text;
    }

    private ChatResponse createResponse(WsMessageContext context, String content) {
        // TODO Lv 13: 현재 연결의 사용자로 저장하고 명세에 맞는 응답을 만듭니다.
        ChatMessageResponse chatMessageResponse  = chatService.saveMessage(context.worldId(), context.nickname(), content);


        return new ChatResponse(
                chatMessageResponse.getSender(),
                chatMessageResponse.getContent(),
                chatMessageResponse.getCreatedAt()
        );
    }
}

```

## 14. 같은 월드의 참여자에게 채팅

브로드캐스트로 해당 월드id의 모든 websocketsession 의 플레이어에게 채팅을 전송한다.

```
@Service
@RequiredArgsConstructor
public class LocalChatSender {
    private final WorldBroadcaster broadcaster;

    public void send(Long worldId, Object message) {
        // TODO Lv 14: 같은 월드의 참여자에게 메시지를 전송합니다.
        broadcaster.broadcast(worldId, message);
    }
}

```

## 15. 접속자 목록 조회
type 이 onlineUsers 인 요청을 받았을 경우 사용되는 핸들러.
context 에서 worldId를 받고 registry 에서 해당 월드의 세션들을 가져온다.
그중 isOpen() 인 사용자만 필터링하고 그 사용자들의 닉네임은 websocketSession.getAttributes().get(NicknameHandshakeInterceptor.ATTR_NICKNAME) 로 가져온다. (인터셉터에서 static 필드로 저장한 이유가 여기 있었다.)

가져온 목록은 요청을 한 사용자에게 브로드캐스트를 통해 메세지를 보내 보여준다.

```
@Override
    public void handle(WsMessageContext context, JsonNode message) {
        // TODO Lv 15: 현재 월드의 열린 연결에서 닉네임을 조회하고 요청자에게 응답합니다.
        List<SessionRegistry.Entry> entries = registry.entries(context.worldId()).stream()
                .filter(entry -> entry.session().isOpen()) //isOpen()이 true인 세션만 선별
                .toList();

        List<String> users = entries.stream()
                .map(SessionRegistry.Entry::session)
                .map(session -> (String) session.getAttributes().get(NicknameHandshakeInterceptor.ATTR_NICKNAME))
                .sorted()
                .toList();

        OnlineUsersResponse onlineUsersResponse = new OnlineUsersResponse(users, users.size());

        broadcaster.sendTo(context.session(), onlineUsersResponse);
    }

```


## 16. 낙관적 락
@Version 어노테이션으로 낙관적 락을 구현 

```
//전
private long revision;

//후
@Version private long revision;

```

## 17. 커서 페이지 조회

커서는 테이블의 데이터와 대조할 목적으로 사용되는 데이터이다. 
JPQL 에선 매개변수로 데이터를 넣어 커서를 사용하였다. 


```
// TODO Lv 17: 다음 페이지가 있으면 반환한 마지막 항목을, 없으면 null을 선택합니다.
        ChatHistoryEntry last = hasNext ? items.getLast() : null; //hasNext 가 true 라면 items 의 마지막 요소를 가져오고 아니라면 null

```

## 18. Redis 최근 채팅 캐시

```
public List<ChatMessageResponse> read(Long worldId, int limit) {
        try {
            // TODO Lv 18: 해당 키의 JSON 문자열을 Redis에서 조회합니다.

            String json = redis.opsForValue().get(key(worldId,limit));
            return json == null ? null : Arrays.asList(mapper.readValue(json, ChatMessageResponse[].class));
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    public void write(Long worldId, int limit, List<ChatMessageResponse> messages) {
        try {
            String json = mapper.writeValueAsString(messages);
            // TODO Lv 18: json을 Redis에 저장하고 5초의 TTL을 설정합니다.
            redis.opsForValue().set(key(worldId,limit), json, Duration.ofSeconds(5));
        } catch (RuntimeException unavailable) {
            // 캐시는 보조 저장소이므로 DB 조회 결과를 그대로 응답합니다.
        }
    }

```


## 19. luaScript 를 활용하여 원자성 확립
allow() 의 목적은 채팅을 보낸 사용자의 최근 채팅량이 제한량에 도달하면 잠시동안 막는 역할이다.
하지만 Redis는 @Transactional 어노테이션을 사용하여 원자성을 확보할 수 없다.

따라서 이전 코드는 redisTemplate.opsForValue().get(key);
redisTemplate.opsForValue().increment(key);
이 두 부분 사이에 중복하여 요청이 발생하였을 때 조건이 있더라도 redis에 반영되기 전의 기준이라 제한량을 초과해버리는 것을 테스트 코드로 확인하였다.

그래서 luaScript 를 통해 redisTemplate.opsForValue().get(key); 
redisTemplate.opsForValue().increment(key); 와 제한 조건들을 
하나의 스크립트로 묶어 처리한다. 

테스트 코드도 정상적으로 채팅량이 제한량을 넘지 못하는 것을 확인하였다.

```
//전
public boolean allow(Long playerId) {
        String key = "chat:limit:" + playerId;
        String value = redisTemplate.opsForValue().get(key);
        int count = value == null ? 0 : Integer.parseInt(value);
        if (count >= 5) {
            return false;
        }
        // TODO Lv 19: 횟수 확인부터 최초 만료 설정까지 원자적으로 실행합니다.
        Long updated = redisTemplate.opsForValue().increment(key);
        if (updated == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(10));
        }
        return true;
    }

//후
public ChatRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();

        setScript();
    }

    private void setScript(){
        script.setScriptText("""
                         --매개변수 List.of(key) 에 해당
                        local key = KEYS[1]
                        --매개변수 "5"에 해당
                        local limit = tonumber(ARGV[1])
                        --매개변수 "10" 해당
                        local ttlSeconds = tonumber(ARGV[2])
                
                        --redisTemplate.opsForValue().get(key) 에 해당
                        local current = tonumber(redis.call('GET', key) or '0') 
                        if current >= limit then     
                            return 0
                        end
                
                
                        --redisTemplate.opsForValue().increment(key); 에 해당
                        local updated = redis.call('INCR', key) 
                        if updated == 1 then   
                            redis.call('EXPIRE', key, ttlSeconds)
                        end
                
                        return 1
                """);
        script.setResultType(Long.class);
    }

    public boolean allow(Long playerId) {
        String key = "chat:limit:" + playerId;

        // TODO Lv 19: 횟수 확인부터 최초 만료 설정까지 원자적으로 실행합니다.
        Long result = redisTemplate.execute(script, List.of(key), MESSAGE_LIMIT, TTL);
        return result == 1;

    }


```
