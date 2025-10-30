# JChat

JChat은 Paper 서버와 Velocity 프록시를 동시에 지원하는 한국어 중심의 멀티 서버 채팅 플러그인입니다. 전역/지역/관리자 채널과 귓속말을 네트워크 전체로 중계하고, 플레이어의 채팅 모드와 색상 사용 권한을 세밀하게 제어합니다.

## 구성요소

| 모듈 | 설명 |
| --- | --- |
| `jchat-paper` | Paper 1.19+ 서버용 메인 플러그인. 채팅 포맷팅, 모드 전환, HQService Netty 및 플러그인 채널 연동을 담당합니다. |
| `jchat-proxy` | Velocity 프록시용 부가 플러그인. Paper 인스턴스 간 메시지를 중계하고 필터링/라우팅/뮤트 기능을 제공합니다. |
| `jchat-common` | Paper와 Velocity가 공유하는 직렬화 및 유틸리티(색상 코드 변환 등)를 담고 있습니다. |

## 주요 기능

- **세 가지 채팅 모드**: `/전체`, `/지역`, `/관리자` 명령으로 전환하며, 플레이어별로 마지막 모드를 저장합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/command/GlobalCommand.kt†L1-L16】【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/ChatModeService.kt†L1-L29】
- **프록시 연동 전역 방송**: 동일 네트워크에 연결된 Paper 서버 사이에서 전역/관리자 채팅과 공지, 귓속말을 공유합니다. HQService Netty가 감지되면 자동 사용하고, 그렇지 않으면 지정된 플러그인 채널로 전송합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/GlobalMessenger.kt†L1-L104】
- **색상 코드 제어**: OP 또는 콘솔만이 `&#RRGGBB`, `<color:#RRGGBB>`와 같은 MiniMessage 태그 및 `&7` 형식의 레거시 색상 코드를 사용할 수 있으며, 일반 플레이어가 보낸 색상 태그는 자동으로 이스케이프됩니다. 【F:jchat-common/src/main/kotlin/kr/jjory/jchat/common/ColorCodeFormatter.kt†L1-L68】【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/listener/ChatListener.kt†L93-L113】
- **PlaceholderAPI 및 LuckPerms 연동**: 메시지에서 PlaceholderAPI 변수를 치환하고, LuckPerms의 접두사를 채팅 포맷에 포함합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/listener/ChatListener.kt†L93-L105】【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/PrefixResolver.kt†L1-L12】
- **귓속말 및 답장**: 동일 서버 또는 다른 서버의 플레이어에게 귓속말을 보내고 `/답장` 명령으로 마지막 대상에게 빠르게 회신합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/command/WhisperCommand.kt†L1-L52】【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/WhisperService.kt†L1-L47】
- **로컬 채팅 반경 및 로깅**: `chat.local-distance` 설정값 안에 있는 플레이어에게만 지역 채팅을 전달하고, `plugins/JChat/logs/` 폴더에 메시지를 날짜별 YAML 파일로 기록합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/listener/ChatListener.kt†L99-L108】【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/MessageLogManager.kt†L1-L12】
- **Velocity 측 필터링과 라우팅**: 욕설 필터, 서버별 방송 허용 목록, 귓속말 대상 서버 전달, `/jchatproxy` 관리 명령을 제공합니다. 【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/PluginMessaging.kt†L32-L102】【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/Moderation.kt†L1-L12】【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/ProxyCommands.kt†L1-L44】

## 빌드 방법

Gradle 래퍼가 포함되어 있으므로 다음 명령으로 Paper/Velocity 플러그인 JAR을 생성할 수 있습니다.

```bash
./gradlew :jchat-paper:build :jchat-proxy:build
```

성공 시 `jchat-paper/build/libs/jchat-paper-<version>.jar`와 `jchat-proxy/build/libs/jchat-proxy-<version>.jar`가 생성됩니다. 【F:jchat-paper/build.gradle.kts†L1-L24】【F:jchat-proxy/build.gradle.kts†L1-L24】

## 설치 및 설정

1. **Paper 서버**
    - `jchat-paper` JAR을 `plugins/` 폴더에 넣고 서버를 재시작합니다.
    - 처음 실행하면 `plugins/JChat/config.yml`이 생성됩니다. `server-id`가 비어 있으면 서버 이름으로 자동 채워집니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/ConfigService.kt†L1-L18】
    - 여러 Paper 서버를 연결한다면 `plugin-channel` 값이 Velocity 설정과 동일한지 확인합니다. 【F:jchat-paper/src/main/resources/config.yml†L1-L18】

2. **Velocity 프록시**
    - `jchat-proxy` JAR을 `plugins/` 폴더에 넣고 프록시를 재시작합니다.
    - `plugins/jchatproxy/config.yml`이 생성됩니다. 로그 미러링, 욕설 필터 패턴, 라우팅 허용 서버 목록 등을 필요에 맞게 수정합니다. 【F:jchat-proxy/src/main/resources/config.yml†L1-L21】【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/ProxyConfig.kt†L1-L63】
    - Paper와 Velocity 모두에서 동일한 `plugin-channel` 이름을 사용해야 메시지가 정상적으로 중계됩니다. 【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/Router.kt†L1-L18】

3. **선택 사항**
    - HQService Netty(예: `kr.hqservice.framework.netty`)가 존재하면 자동으로 바인딩하여 별도 설정 없이 네트워크 전송에 사용합니다. 없을 경우 플러그인 채널을 통해 통신합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/GlobalMessenger.kt†L11-L58】
    - PlaceholderAPI, LuckPerms를 설치하면 채팅에 변수를 사용하고 접두사를 자동으로 가져옵니다. 설치되어 있지 않아도 플러그인이 동작합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/listener/ChatListener.kt†L93-L105】【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/PrefixResolver.kt†L1-L12】

## 사용 방법

### Paper 명령어

| 명령 | 설명 | 권한 |
| --- | --- | --- |
| `/전체` | 채팅 모드를 전역으로 전환합니다. | 기본 |
| `/지역` | 설정한 반경 내 플레이어에게만 보이는 지역 채팅 모드로 전환합니다. | 기본 |
| `/관리자` | 관리자 채널로 전환합니다. | `jchat.admin` |
| `/공지 <메시지>` | 네트워크 전체에 공지합니다. PlaceholderAPI가 적용되며, 색상은 OP/콘솔만 사용 가능합니다. | `jchat.admin` |
| `/귓 <플레이어> <메시지>` | 대상 플레이어에게 귓속말을 보냅니다. 다른 서버에 있으면 자동으로 프록시를 통해 전달됩니다. | 기본 |
| `/답장 <메시지>` | 마지막으로 귓속말을 주고받은 플레이어에게 회신합니다. | 기본 |
| `/채팅리로드` | `config.yml`을 리로드합니다. | `jchat.admin` (명령어 권한 설정에 따라 조정) |

각 명령은 `plugin.yml`에 등록되어 있으며, Paper에서 한글 명령 라벨을 그대로 사용합니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/JChat.kt†L5-L45】【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/command/AnnounceCommand.kt†L1-L35】

### Velocity 명령어

| 명령 | 설명 | 권한 |
| --- | --- | --- |
| `/jchatproxy mute <UUID|이름>` | 지정한 플레이어를 네트워크 전역에서 음소거합니다. | 프록시 운영자 |
| `/jchatproxy unmute <UUID|이름>` | 음소거를 해제합니다. | 프록시 운영자 |
| `/jchatproxy announce <메시지>` | 프록시에서 직접 공지를 전송합니다. 색상 코드는 콘솔/`jchat.admin` 권한이 있는 OP만 사용할 수 있습니다. | 프록시 운영자 |

명령은 Velocity `CommandManager`에 등록되며, `/jchatproxy` 기본 도움말에서 사용 방법을 확인할 수 있습니다. 【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/ProxyCommands.kt†L1-L44】

## 색상 사용 정책

- 플레이어 채팅과 귓속말, 공지는 MiniMessage 문법을 사용합니다.
- OP 또는 콘솔만 `&a`, `§6`, `<#12abff>`, `<color:gold>` 등 색상/스타일 태그를 적용할 수 있습니다. 일반 플레이어가 이런 태그를 입력하면 자동으로 이스케이프되어 그대로 출력됩니다.
- Velocity에서 `/jchatproxy announce`를 사용하는 경우에도 동일한 정책이 적용되며, `jchat.admin` 권한을 가진 플레이어만 색상 코드를 사용할 수 있습니다. 【F:jchat-common/src/main/kotlin/kr/jjory/jchat/common/ColorCodeFormatter.kt†L1-L68】【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/ProxyCommands.kt†L21-L32】

## 데이터 저장 경로

- 플레이어별 채팅 모드는 `plugins/JChat/playerdata/<uuid>.yml` 파일에 저장되어 접속 시 유지됩니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/PlayerDataService.kt†L1-L10】
- 프록시의 플레이어 인덱스와 뮤트 목록은 메모리에 유지되며, 재시작 시 `config.yml`의 설정값을 기반으로 초기화됩니다. 【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/StateStore.kt†L1-L18】【F:jchat-proxy/src/main/kotlin/kr/jjory/jchatproxy/ProxyConfig.kt†L47-L58】

## 문제 해결

- 공지가 두 번 표시되면 Paper 서버에서 Velocity로의 전송이 실패했는지 확인하십시오. 플러그인 채널 이름이 일치하고, 서버에 최소 1명의 플레이어가 접속해 있어야 전송 캐리어가 확보됩니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/PluginChannelMessenger.kt†L1-L23】
- HQService Netty 연동 실패 로그가 출력되면 플러그인이 자동으로 플러그인 채널 모드로 전환하므로 추가 조치는 필요하지 않습니다. 【F:jchat-paper/src/main/kotlin/kr/jjory/jchat/service/GlobalMessenger.kt†L24-L58】

## 라이선스

이 저장소에는 라이선스 파일이 포함되어 있지 않습니다. 배포 또는 수정에 대한 정책을 확인하려면 저장소 소유자에게 문의하십시오.