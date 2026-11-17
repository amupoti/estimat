# Banc de preguntes (CA/ES)

Les dades són dins de l'app, perquè s'empaqueten a l'APK:

- [`app/src/main/assets/questions/questions.json`](../app/src/main/assets/questions/questions.json): les preguntes.
- [`app/src/main/assets/questions/tags.json`](../app/src/main/assets/questions/tags.json): la taxonomia de tags, amb les etiquetes en català i castellà per mostrar-les a l'app.
- `validate.py`: comprova l'esquema i els tags i mostra estadístiques (`python3 questions/validate.py`).

## Format d'una pregunta

```json
{
  "id": "cat-009",
  "pregunta": { "ca": "Quantes comarques té Catalunya?", "es": "¿Cuántas comarcas tiene Cataluña?" },
  "resposta": 43,
  "unitat": { "ca": "comarques", "es": "comarcas" },
  "tags": ["catalunya", "mitja", "geografia", "divertit"],
  "explicacio": { "ca": "…", "es": "…" },
  "font": "https://…",
  "any_referencia": 2023,
  "confianca": "alta",
  "estat": "pendent"
}
```

| Camp | Notes |
|---|---|
| `id` | `cat-NNN`, `esp-NNN` o `int-NNN`. No es reutilitzen mai, encara que la pregunta s'esborri. |
| `resposta` | Sempre un número (enter o decimal). Si la xifra és gran, és millor posar-la en milions a la unitat (`55` + "milions de passatgers") que escriure `55000000`. |
| `unitat` | Pot ser `""` quan la pregunta ja diu la unitat ("Quin número de papa…"). |
| `explicacio` | Opcional (`null`). És la curiositat que es mostra en revelar la resposta. |
| `any_referencia` | `null` si la dada no canvia amb el temps. Si n'hi ha, la pregunta s'ha de revisar periòdicament. |
| `confianca` | Grau de seguretat de la dada abans de verificar-la: `alta`, `mitjana` o `baixa`. |
| `estat` | `pendent` → `verificat` (o `rebutjat`). L'app les mostra totes per defecte; a Configuració es pot activar "Només preguntes verificades". Les `rebutjat` no es mostren mai. |

## Tags

Cada pregunta té:
- **1 àmbit**: `catalunya`, `espanya`, `internacional`
- **1 dificultat**: `facil`, `mitja`, `dificil`
- **1 tema o més**: `geografia`, `natura`, `cos-huma`, `ciencia`, `historia`, `arquitectura`, `art`, `musica`, `cinema-tv`, `esport`, `gastronomia`, `tradicions`, `transport`, `tecnologia`, `jocs`, `societat`
- **públic (opcional)**: `nens` (pensada per a infants) o `adults` (+18). Sense cap tag de públic, la pregunta és per a adults en general.
- **to (opcional, un de sol)**:
  - `divertit`: tema per riure o absurd: pets, caca, mocs, rècords de menjar, Guinness estrambòtics. *Quants calçots es va menjar el rècord de Valls?*
  - `sorprenent`: dada seriosa amb una resposta inesperada. *Quantes vèrtebres té el coll d'una girafa?*
  - Si és totes dues coses, posa `divertit`.

Per filtrar a l'app, n'hi ha prou amb comprovar si `tags` conté els tags seleccionats. Suggeriment de filtre per defecte: amagar `adults` si no s'activa explícitament, i en mode "nens" mostrar només les que tenen `nens`.

### Com es decideix la dificultat
Depèn de **com de probable és saber la xifra exacta** i de **com de fàcil és acostar-s'hi**, no de com d'obscura és la dada:
- **fàcil**: molta gent la sap o la dedueix (206 ossos). Per a nens o per escalfar.
- **mitjà**: gairebé ningú la sap exacta, però tothom n'encerta l'ordre de magnitud (Tibidabo, 512 m).
- **difícil**: ningú la sap, i les respostes poden variar fins a 10 vegades, però es pot raonar (illes de l'Eixample, 113 m).

## Procés editorial

1. **Proposar.** Escriu la pregunta en català i en castellà, amb `estat: "pendent"`. Abans de continuar, comprova que:
   - la resposta és un sol número, sense ambigüitat (precisa "sense pedestal", "a l'equador", "sumant totes les categories"…);
   - no la sap gairebé ningú exacta, però es pot estimar;
   - no és una pregunta de demografia més (aquest tipus no hauria de passar del 10% del banc).
2. **Verificar.** Obre la `font` i confirma la xifra. Si dues fonts fiables no coincideixen, tria la més oficial (organisme públic > entitat gestora > Wikipedia) i explica-ho a l'`explicacio`, o reformula la pregunta perquè no hi hagi dubte. Si la dada no es pot confirmar, marca la pregunta com a `rebutjat`.
3. **Marcar.** Canvia l'estat a `verificat` i, si cal, actualitza `font` i `any_referencia`.
4. **Validar.** Executa `python3 questions/validate.py`: no ha de mostrar cap error.
5. **Revisar periòdicament.** Un cop l'any, revisa les preguntes amb `any_referencia` de dades que canvien (rècords, passatgers, títols esportius).
6. **Provar a la taula.** Després de cada partida, apunta les preguntes que han funcionat malament (algú se la sabia, o ningú s'hi va acostar) i ajusta'n la dificultat o retira-les.

## Estat actual

145 preguntes (32 de Catalunya, 32 d'Espanya i 81 internacionals), de les quals 23 són `divertit`. Només 2 estan `verificat` (el rècord de calçots i el de l'esternut); la resta són `pendent`. Prioritat de verificació: primer les de `confianca: baixa`, després les de `mitjana`.

Què falta per arribar a 200: més preguntes de Catalunya i d'Espanya (sobretot de tradicions, gastronomia, música i societat), més preguntes de `tecnologia` i `transport`, i més preguntes `facil` per a nens.

### Idees divertides pendents de dada
Encara no hi ha una xifra fiable per a aquestes idees, i per això no són al JSON. No hi posis un número fins que trobis la font.
- Quants quilos de xocolata es venen a Espanya per Sant Valentí? (Produlce no publica xifres en quilos. La xifra que circula, uns 26 milions de kg, és dels EUA i no té una font clara.)
- Quants panellets es venen a Catalunya per Tots Sants? (Gremi de Pastisseria)
- Quants caganers diferents es fabriquen cada any?
- Quants quilos pesava la truita de patates més gran del món? (Guinness)
- Quants raïms es mengen a Espanya per Cap d'Any?
- Rècords de concursos populars: menjar panellets, escopir pinyols d'oliva, llançament d'espardenya…
