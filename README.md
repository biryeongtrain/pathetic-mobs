# Pathetic Mobs

Fabric 환경에서 Minecraft mob pathfinding 일부를 [Pathetic](https://github.com/bsommerfeld/pathetic) 기반 탐색으로 대체해 성능을 개선하는 실험적 모드입니다. 구현 방향은 `pathetic-bukkit`의 provider/context/processor 구조를 Fabric에 맞게 옮기고, vanilla `WalkNodeEvaluator` 의존을 줄이는 쪽에 맞춰져 있습니다.

## 목표

- 단일 target을 향해 이동하는 ground mob pathfinding을 더 빠르게 처리합니다.
- block collision, floor support, hazard penalty를 Fabric/Minecraft world state에서 직접 평가합니다.
- 단순 직선 이동, 벽 우회, hazard 회피, 좁은 통로, 실제 게임형 복합 장애물에서 vanilla와 Pathetic 경로 생성 시간을 비교합니다.
- 개발 환경에서는 vanilla 방식과 Pathetic 방식을 쓰는 엔티티를 동시에 소환해 눈으로 비교할 수 있게 합니다.

## 지원 환경

- Minecraft `1.21.8`
- Fabric Loader `0.19.2`
- Fabric API `0.136.1+1.21.8`
- Java `21`
- Pathetic API/Engine `5.4.6`

## 동작 방식

`PathFinderMixin`이 vanilla `PathFinder#findPath` 진입 지점에서 조건을 확인한 뒤, 다음 경우에만 Pathetic 기반 경로를 시도합니다.

- evaluator가 `WalkNodeEvaluator`인 ground mob pathfinding
- target이 하나인 pathfinding 요청
- 디버그/벤치마크용 vanilla 우회 대상이 아닌 mob

Pathetic 경로 생성은 다음 순서로 처리됩니다.

1. 같은 높이에서 장애물과 penalty가 없는 direct clear-line fast path 시도
2. 단순 벽 우회를 위한 bounded orthogonal dogleg fast path 시도
3. 위 두 fast path가 실패하면 Pathetic A* 실행
4. Pathetic 결과가 없거나 부적절하면 vanilla pathfinder가 계속 처리하도록 fallback

## 핵심 구현

- `FabricNavigationPointProvider`
  - headroom collision, floor support, hazard block을 직접 평가합니다.
  - lava, fire, campfire, cactus, magma block, sweet berry bush 등은 avoid cost를 부여합니다.
- `PatheticPathfinding`
  - Pathetic `AStarPathfinderFactory`와 horizontal cardinal neighbor strategy를 사용합니다.
  - validation/cost processor에서 `FabricNavigationPointProvider`를 직접 호출합니다.
  - 직선/단순 우회 fast path는 모든 후보 node가 traversable이고 penalty가 0일 때만 반환합니다.
- `PathfindingBenchmarkControl`
  - GameTest에서 vanilla/Pathetic을 같은 JVM 안에서 번갈아 측정합니다.
  - 개발 커맨드로 소환한 특정 mob만 vanilla pathfinding을 쓰도록 등록할 수 있습니다.

## 개발용 비교 커맨드

이 커맨드는 `FabricLoader.getInstance().isDevelopmentEnvironment()`가 `true`일 때만 등록됩니다.

```mcfunction
/pathetic pathdebug spawn <entity_type> <pos>
```

예시:

```mcfunction
/pathetic pathdebug spawn minecraft:zombie ~ ~ ~
```

동작:

- `<pos>` 주변에 같은 entity type의 mob 두 마리를 소환합니다.
- `[Vanilla] minecraft:zombie`
  - Pathetic mixin을 우회하고 vanilla pathfinding을 사용합니다.
- `[Pathetic] minecraft:zombie`
  - 현재 패치된 Pathetic pathfinding을 사용합니다.
- 두 엔티티 모두 custom name이 항상 보이도록 설정됩니다.

`entity_type`은 summon 가능한 `Mob`이어야 합니다. projectile, item, display entity처럼 pathfinding을 하지 않는 entity type은 거부됩니다.

## 벤치마크 / GameTest

GameTest는 `src/gametest/java/kim/biryeong/pathetic/PathfindingGameTest.java`에 있습니다.

실행:

```bash
./gradlew runGameTest --console=plain --no-daemon
```

전체 검증:

```bash
./gradlew clean build --console=plain --no-daemon
```

각 scenario는 vanilla와 Pathetic 경로 생성을 같은 조건에서 측정합니다.

- warmup: 5회
- sample: 24회
- 측정값:
  - `vanillaMedianNs`: vanilla 중앙값 실행 시간
  - `patheticMedianNs`: Pathetic 중앙값 실행 시간
  - `medianRatio`: `patheticMedianNs / vanillaMedianNs`
  - `vanillaP95Ns`: vanilla 95퍼센타일 실행 시간
  - `patheticP95Ns`: Pathetic 95퍼센타일 실행 시간
  - `p95Ratio`: `patheticP95Ns / vanillaP95Ns`
  - `vanillaNodes`, `patheticNodes`: 생성된 path node 수

`medianRatio`와 `p95Ratio`는 1.0보다 낮을수록 Pathetic 쪽이 빠릅니다.

## 현재 벤치마크 시나리오

- `flat_short`
  - 짧은 평지 직선 이동
- `flat_wide`
  - 넓은 평지에서 대각선성 목표 이동
- `wall_detour`
  - 중앙 벽을 피해 돌아가는 단순 우회
- `hazard_avoid`
  - magma block penalty를 피해 이동
- `narrow_corridor`
  - 좌우 벽이 있는 좁은 통로
- `enclosed_goal_back_entrance`
  - 도착 지점을 벽으로 감싸고, 엔티티 반대편 벽에만 입구를 둔 구조
- `two_gate_hazard_detour`
  - 두 개의 offset wall/gate와 hazard가 섞인 복합 우회
- `courtyard_backtrack`
  - 외벽, 내부 분리벽, hazard 때문에 실제 게임처럼 되돌아 들어가야 하는 courtyard 구조

최근 clean build 기준 결과:

| Scenario | medianRatio | p95Ratio | Nodes V/P |
|---|---:|---:|---:|
| `flat_short` | `0.074` | `0.162` | 6 / 6 |
| `flat_wide` | `0.089` | `0.099` | 6 / 6 |
| `wall_detour` | `0.066` | `0.081` | 7 / 10 |
| `hazard_avoid` | `0.551` | `0.693` | 7 / 9 |
| `narrow_corridor` | `0.266` | `0.556` | 6 / 6 |
| `enclosed_goal_back_entrance` | `0.526` | `0.581` | 12 / 15 |
| `two_gate_hazard_detour` | `0.644` | `0.556` | 13 / 19 |
| `courtyard_backtrack` | `0.687` | `0.688` | 24 / 29 |

Aggregate:

- `worstMedianScenario=courtyard_backtrack`
- `worstMedianRatio=0.687`
- `worstP95Scenario=hazard_avoid`
- `worstP95Ratio=0.693`

벤치마크 수치는 로컬 실행 환경, JVM warmup, 서버 tick 상태에 따라 변동될 수 있습니다. 추세 판단은 단일 실행보다 여러 번의 clean build/GameTest 반복을 기준으로 보는 것이 안전합니다.

## 빌드

```bash
./gradlew build
```

빌드 결과물은 `build/libs/` 아래에 생성됩니다.

## 라이선스

이 프로젝트는 `LICENSE` 파일의 라이선스를 따릅니다.
