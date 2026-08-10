<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, type CreationConfigurationPayload } from './services/api';
import CharacterSheet from './CharacterSheet.vue';

type Campaign = { id: string; name: string; createdAt: string };
type Character = { id: string; name: string; imageUrl?: string | null; campaignId?: string };
type CreationMode = 'empty' | 'guided';
type GuidedStep = 'setup' | 'race' | 'majors' | 'einherjer' | 'awakened' | 'complete';

const campaigns = ref<Campaign[]>([]);
const characters = ref<Character[]>([]);
const selectedCampaign = ref<Campaign | null>(null);
const isCampaignModalOpen = ref(false);
const isCharacterModalOpen = ref(false);
const isDeleteCampaignModalOpen = ref(false);
const campaignName = ref('');
const characterName = ref('');
const characterImage = ref('');
const imageInput = ref<HTMLInputElement | null>(null);
const creationMode = ref<CreationMode>('empty');
const guidedStep = ref<GuidedStep>('setup');
const guidedCharacterId = ref<string | null>(null);
const guidedRace = ref('');
const guidedEinherjer = ref<boolean | null>(null);
const guidedAwakened = ref<boolean | null>(null);
const guidedEinherjerOrigin = ref<'converted' | 'born_human' | 'born_einherjer' | null>(null);
const guidedStartingAge = ref<number | null>(null);
const guidedAwakeningAge = ref<number | null>(null);
const guidedSheetAge = ref<number | null>(null);
const selectedMajorAttributes = ref<string[]>([]);
const majorAttributes = [
  { key: 'fisico', label: 'Físico' }, { key: 'agilidad', label: 'Agilidad' },
  { key: 'percepcion', label: 'Percepción' }, { key: 'mente', label: 'Mente' },
  { key: 'estudio', label: 'Estudio' }, { key: 'carisma', label: 'Carisma' },
];
const loading = ref(true);
const saving = ref(false);
const error = ref('');
const isDirector = ref(localStorage.getItem('dexm.director') === 'true');
const route = useRoute();
const router = useRouter();
const isCharacterSheet = computed(() => Boolean(route.params.id));

const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
const campaignCountLabel = computed(() => `${campaigns.value.length} ${campaigns.value.length === 1 ? 'campaña' : 'campañas'}`);

async function loadCampaigns() {
  loading.value = true;
  try {
    campaigns.value = await api.campaigns();
    const campaignId = String(route.query.campaign || '');
    const campaign = campaigns.value.find(item => item.id === campaignId);
    if (campaign) await selectCampaign(campaign);
  } catch (e: any) { error.value = e.message; } finally { loading.value = false; }
}
async function selectCampaign(campaign: Campaign) {
  selectedCampaign.value = campaign;
  try { characters.value = await api.characters(campaign.id); error.value = ''; } catch (e: any) { error.value = e.message; }
}
function persistDirector() { localStorage.setItem('dexm.director', String(isDirector.value)); }
function toggleDirector() { isDirector.value = !isDirector.value; persistDirector(); if (!isDirector.value && isCampaignModalOpen.value) closeModals(); }
function resetCharacterCreation() {
  characterName.value = ''; characterImage.value = ''; creationMode.value = 'empty'; guidedStep.value = 'setup';
  guidedCharacterId.value = null; guidedRace.value = ''; guidedEinherjer.value = null; guidedAwakened.value = null; guidedEinherjerOrigin.value = null; guidedStartingAge.value = null; guidedAwakeningAge.value = null; guidedSheetAge.value = null; selectedMajorAttributes.value = [];
  if (imageInput.value) imageInput.value.value = '';
}
function openCampaignModal() { campaignName.value = ''; isCampaignModalOpen.value = true; }
function openCharacterModal() { resetCharacterCreation(); isCharacterModalOpen.value = true; }
function closeModals() { isCampaignModalOpen.value = false; isCharacterModalOpen.value = false; }
function openDeleteCampaignModal() { isDeleteCampaignModalOpen.value = true; }
function closeDeleteCampaignModal() { isDeleteCampaignModalOpen.value = false; }
async function createCampaign() {
  if (!isDirector.value || !campaignName.value.trim()) return;
  saving.value = true;
  try { const campaign = await api.createCampaign(campaignName.value.trim()); await loadCampaigns(); await selectCampaign(campaign); closeModals(); } catch (e: any) { error.value = e.message; } finally { saving.value = false; }
}
async function createCharacter() {
  if (!selectedCampaign.value || !characterName.value.trim()) return;
  saving.value = true;
  try {
    const created = await api.createCharacter(selectedCampaign.value.id, { name: characterName.value.trim(), imageUrl: characterImage.value || null }) as Character;
    const initial: CreationConfigurationPayload = creationMode.value === 'empty'
      ? { mode: 'empty', einherjer: true, awakened: guidedAwakened.value, einherjerOrigin: guidedEinherjerOrigin.value, awakeningAge: guidedAwakened.value ? guidedAwakeningAge.value : null, sheetAge: guidedSheetAge.value, selectedMajorAttributes: [], wizardState: 'complete' }
      : { mode: 'guided', selectedMajorAttributes: [], wizardState: 'started' };
    await api.configureCreation(created.id, initial);
    characters.value = await api.characters(selectedCampaign.value.id);
    if (creationMode.value === 'empty') closeModals();
    else { guidedCharacterId.value = created.id; guidedStep.value = 'race'; }
  } catch (e: any) { error.value = e.message; } finally { saving.value = false; }
}
async function saveGuidedConfiguration(configuration: Omit<CreationConfigurationPayload, 'mode'>) {
  if (!guidedCharacterId.value) return false;
  saving.value = true;
  try {
    await api.configureCreation(guidedCharacterId.value, { mode: 'guided', ...configuration });
    return true;
  } catch (e: any) { error.value = e.message; return false; } finally { saving.value = false; }
}
async function answerGuidedRace() {
  if (guidedStartingAge.value === null || guidedSheetAge.value === null || guidedSheetAge.value <= guidedStartingAge.value) return;
  if (await saveGuidedConfiguration({ race: guidedRace.value, startingAge: guidedStartingAge.value, sheetAge: guidedSheetAge.value, selectedMajorAttributes: [], wizardState: 'race' })) guidedStep.value = 'majors';
}
function toggleMajorAttribute(key: string) {
  if (selectedMajorAttributes.value.includes(key)) selectedMajorAttributes.value = selectedMajorAttributes.value.filter(item => item !== key);
  else if (selectedMajorAttributes.value.length < 2) selectedMajorAttributes.value = [...selectedMajorAttributes.value, key];
}
async function answerGuidedMajors() {
  if (selectedMajorAttributes.value.length !== 2) return;
  if (await saveGuidedConfiguration({ race: guidedRace.value, startingAge: guidedStartingAge.value, sheetAge: guidedSheetAge.value, selectedMajorAttributes: selectedMajorAttributes.value, wizardState: 'majors' })) guidedStep.value = 'einherjer';
}
async function answerGuidedEinherjer(value: boolean) {
  guidedEinherjer.value = value;
  const wizardState = value ? 'einherjer' : 'complete';
  if (await saveGuidedConfiguration({ race: guidedRace.value, einherjer: value, awakened: null, einherjerOrigin: null, startingAge: guidedStartingAge.value, sheetAge: guidedSheetAge.value, selectedMajorAttributes: selectedMajorAttributes.value, wizardState })) guidedStep.value = value ? 'awakened' : 'complete';
}
async function answerGuidedAwakened(value: boolean) {
  guidedAwakened.value = value;
  if (guidedEinherjerOrigin.value === null || (value && (guidedAwakeningAge.value === null || guidedAwakeningAge.value < (guidedStartingAge.value ?? 0) || guidedAwakeningAge.value > (guidedSheetAge.value ?? 0)))) return;
  if (await saveGuidedConfiguration({ race: guidedRace.value, einherjer: true, awakened: value, einherjerOrigin: guidedEinherjerOrigin.value, awakeningAge: value ? guidedAwakeningAge.value : null, startingAge: guidedStartingAge.value, sheetAge: guidedSheetAge.value, selectedMajorAttributes: selectedMajorAttributes.value, wizardState: 'complete' })) {
    guidedStep.value = 'complete';
    if (selectedCampaign.value) characters.value = await api.characters(selectedCampaign.value.id);
  }
}
async function deleteCampaign() {
  if (!selectedCampaign.value) return;
  saving.value = true;
  try {
    await api.deleteCampaign(selectedCampaign.value.id);
    closeDeleteCampaignModal();
    selectedCampaign.value = null;
    characters.value = [];
    await loadCampaigns();
  } catch (e: any) { error.value = e.message; } finally { saving.value = false; }
}
function readImage(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const reader = new FileReader(); reader.onload = () => { characterImage.value = String(reader.result); }; reader.readAsDataURL(file);
}
function clearImage() { characterImage.value = ''; if (imageInput.value) imageInput.value.value = ''; }
function openCharacter(character: Character) { router.push(`/characters/${character.id}`); }
onMounted(loadCampaigns);
watch(() => route.query.campaign, (campaignId) => {
  if (!campaignId) return;
  const campaign = campaigns.value.find(item => item.id === String(campaignId));
  if (campaign) selectCampaign(campaign);
});
</script>

<template>
  <CharacterSheet v-if="isCharacterSheet" />
  <main v-else class="app-shell">
    <header class="topbar">
      <div class="brand"><img class="brand-logo" src="/logo.png" alt="Deus ex Machina" /></div>
    </header>

    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>

    <div class="content-grid">
      <aside class="campaign-rail">
        <div class="section-heading"><div><p class="eyebrow">TUS MUNDOS</p><h3>Campañas</h3></div><span class="count">{{ campaignCountLabel }}</span></div>
        <div v-if="loading" class="empty-small">Cargando…</div>
        <button v-for="campaign in campaigns" v-else :key="campaign.id" class="campaign-item" :class="{ active: selectedCampaign?.id === campaign.id }" @click="selectCampaign(campaign)"><span class="campaign-dot"></span><span>{{ campaign.name }}</span><span class="arrow">→</span></button>
        <div v-if="!loading && !campaigns.length" class="empty-small">Aún no tienes campañas.</div>
        <button v-if="isDirector" class="new-campaign-link" @click="openCampaignModal">＋ Crear una campaña</button>
        <button class="director-toggle" :class="{ active: isDirector }" type="button" :aria-pressed="isDirector" aria-label="Activar o desactivar modo Director" @click="toggleDirector"><span class="director-toggle-label">DM</span><span class="director-toggle-knob" aria-hidden="true"></span></button>
      </aside>

      <section v-if="selectedCampaign" class="campaign-view">
        <div class="campaign-heading"><div><p class="eyebrow accent">CAMPAÑA SELECCIONADA</p><h2>{{ selectedCampaign.name }}</h2><p class="muted">{{ characters.length }} {{ characters.length === 1 ? 'personaje' : 'personajes' }} en esta aventura</p></div><div class="campaign-actions"><button v-if="isDirector" class="button button-danger" @click="openDeleteCampaignModal">Borrar campaña</button><button class="button button-primary" @click="openCharacterModal"><span>＋</span> Nuevo personaje</button></div></div>
        <div v-if="characters.length" class="character-grid"><article v-for="character in characters" :key="character.id" class="character-card" role="link" tabindex="0" @click="openCharacter(character)" @keydown.enter="openCharacter(character)"><div class="portrait"><img v-if="character.imageUrl" :src="character.imageUrl" :alt="`Retrato de ${character.name}`" /><span v-else>{{ initials(character.name) }}</span></div><div class="character-info"><p class="eyebrow">PERSONAJE</p><h3>{{ character.name }}</h3><span class="card-link">Ver ficha →</span></div></article></div>
        <div v-else class="empty-state"><div class="empty-icon">✦</div><h3>La aventura está esperando</h3><p>Añade el primer personaje a <strong>{{ selectedCampaign.name }}</strong>.</p><button class="button button-primary" @click="openCharacterModal">＋ Nuevo personaje</button></div>
      </section>
      <section v-else class="welcome-state"><div class="welcome-orbit">✦</div><p class="eyebrow accent">PRIMER PASO</p><h2>Selecciona una campaña</h2><p>Elige un mundo del archivo o crea uno nuevo para comenzar.</p><button v-if="isDirector" class="button button-primary" @click="openCampaignModal">＋ Nueva campaña</button></section>
    </div>

    <div v-if="isCampaignModalOpen || isCharacterModalOpen" class="modal-backdrop" @click.self="closeModals">
      <section class="modal creation-modal" role="dialog" aria-modal="true" :aria-labelledby="isCampaignModalOpen ? 'campaign-modal-title' : 'character-modal-title'">
        <button class="modal-close" aria-label="Cerrar" @click="closeModals">×</button>
        <template v-if="isCampaignModalOpen">
          <p class="eyebrow accent">NUEVO MUNDO</p><h2 id="campaign-modal-title">Crea una campaña</h2><p class="modal-copy">Un nombre basta para abrir un nuevo capítulo.</p>
          <form @submit.prevent="createCampaign"><label>Nombre<input v-model="campaignName" autofocus placeholder="Ej. Las cenizas de Midgard" maxlength="160" /></label><button class="button button-primary modal-submit" :disabled="saving || !campaignName.trim()">{{ saving ? 'Guardando…' : 'Crear campaña' }}</button></form>
        </template>
        <template v-else>
          <p class="eyebrow accent">NUEVO PERSONAJE</p>
          <h2 id="character-modal-title">{{ guidedStep === 'setup' ? 'Da vida a tu personaje' : 'Crea tu personaje paso a paso' }}</h2>
          <p class="modal-copy">{{ guidedStep === 'setup' ? 'Elige cómo quieres comenzar. El modo guiado guardará cada respuesta al avanzar.' : 'Puedes cerrar esta ventana cuando quieras: el estado alcanzado ya está guardado.' }}</p>

          <form v-if="guidedStep === 'setup'" @submit.prevent="createCharacter">
            <label>Nombre<input v-model="characterName" autofocus placeholder="Ej. Astrid la Errante" maxlength="160" /></label>
            <label>Imagen <span class="label-hint">URL o archivo local</span><input v-model="characterImage" placeholder="https://…" /></label>
            <label class="file-input">Subir retrato<input ref="imageInput" type="file" accept="image/*" @change="readImage" /></label>
            <div v-if="characterImage" class="image-preview"><img :src="characterImage" alt="Vista previa del retrato" /><button type="button" @click="clearImage">Quitar imagen</button></div>
            <fieldset class="creation-mode-field"><legend>Modo de creación</legend><div class="creation-mode-grid">
              <label class="creation-mode-option" :class="{ selected: creationMode === 'empty' }"><input v-model="creationMode" type="radio" value="empty" /><strong>Vacío</strong><span>Empieza con todos los valores a cero.</span></label>
              <label class="creation-mode-option" :class="{ selected: creationMode === 'guided' }"><input v-model="creationMode" type="radio" value="guided" /><strong>Guiado</strong><span>Configura raza, atributos y Einherjer con ayuda.</span></label>
            </div></fieldset>
            <fieldset v-if="creationMode === 'empty'" class="creation-mode-field"><legend>Datos Einherjer</legend>
              <label>Origen<select v-model="guidedEinherjerOrigin" required><option :value="null" disabled>Selecciona un origen</option><option value="converted">Convertido</option><option value="born_human">Nacido de humanos</option><option value="born_einherjer">Nacido de Einherjer</option></select></label>
              <label>Edad actual<input v-model.number="guidedSheetAge" type="number" min="0" required /></label>
              <p>¿Ha despertado?</p><div class="guided-answer-grid"><button class="answer-card" type="button" :class="{ selected: guidedAwakened === false }" @click="guidedAwakened = false">No</button><button class="answer-card" type="button" :class="{ selected: guidedAwakened === true }" @click="guidedAwakened = true">Sí</button></div>
              <label v-if="guidedAwakened">Edad de despertar / conversión<input v-model.number="guidedAwakeningAge" type="number" min="0" :max="guidedSheetAge ?? undefined" required /></label>
            </fieldset>
            <button class="button button-primary modal-submit" :disabled="saving || !characterName.trim()">{{ saving ? 'Guardando…' : (creationMode === 'guided' ? 'Comenzar guía' : 'Crear personaje') }}</button>
          </form>

          <div v-else class="guided-flow">
            <div class="guided-progress"><span>PASO GUIADO</span><strong>{{ guidedStep === 'race' ? '1 de 4' : guidedStep === 'majors' ? '2 de 4' : guidedStep === 'einherjer' ? '3 de 4' : guidedStep === 'awakened' ? '4 de 4' : 'Completado' }}</strong></div>
            <div v-if="guidedStep === 'race'" class="guided-question"><h3>¿Cuál es tu raza y qué edades tiene tu historia?</h3><p>La barra temporal usa edades del personaje.</p><button class="answer-card selected" type="button" @click="guidedRace = 'Humano de Midgard'">Humano de Midgard</button><div class="guided-age-grid"><label>Edad inicial<input v-model.number="guidedStartingAge" type="number" min="0" /></label><label>Edad actual<input v-model.number="guidedSheetAge" type="number" min="1" /></label></div><div class="modal-actions"><button class="button button-primary" type="button" :disabled="saving || !guidedRace || guidedStartingAge === null || guidedSheetAge === null" @click="answerGuidedRace">Continuar</button></div></div>
            <div v-else-if="guidedStep === 'majors'" class="guided-question"><h3>Elige 2 atributos mayores</h3><p>El humano de Midgard obtiene +1 en cada atributo seleccionado.</p><div class="major-selection-grid"><button v-for="attribute in majorAttributes" :key="attribute.key" class="answer-card" :class="{ selected: selectedMajorAttributes.includes(attribute.key) }" type="button" @click="toggleMajorAttribute(attribute.key)">{{ attribute.label }}<span>{{ selectedMajorAttributes.includes(attribute.key) ? '✓' : '' }}</span></button></div><p class="selection-counter">{{ selectedMajorAttributes.length }} de 2 seleccionados</p><div class="modal-actions"><button class="button button-primary" type="button" :disabled="saving || selectedMajorAttributes.length !== 2" @click="answerGuidedMajors">Continuar</button></div></div>
            <div v-else-if="guidedStep === 'einherjer'" class="guided-question"><h3>Datos del Einherjer</h3><p>Selecciona el origen y si ha despertado. La edad de despertar solo es necesaria si responde «Sí».</p><div class="guided-answer-grid"><button class="answer-card" :class="{ selected: guidedEinherjerOrigin === 'converted' }" type="button" @click="guidedEinherjerOrigin = 'converted'">Convertido</button><button class="answer-card" :class="{ selected: guidedEinherjerOrigin === 'born_human' }" type="button" @click="guidedEinherjerOrigin = 'born_human'">Nacido de humanos</button><button class="answer-card" :class="{ selected: guidedEinherjerOrigin === 'born_einherjer' }" type="button" @click="guidedEinherjerOrigin = 'born_einherjer'">Nacido de Einherjer</button></div><p>¿Ha despertado?</p><div class="guided-answer-grid"><button class="answer-card" :class="{ selected: guidedAwakened === false }" type="button" :disabled="guidedEinherjerOrigin === null" @click="answerGuidedAwakened(false)">No</button><button class="answer-card" :class="{ selected: guidedAwakened === true }" type="button" :disabled="guidedEinherjerOrigin === null" @click="answerGuidedAwakened(true)">Sí</button></div><label v-if="guidedAwakened">Edad de despertar / conversión<input v-model.number="guidedAwakeningAge" type="number" :min="guidedStartingAge ?? 0" :max="guidedSheetAge ?? undefined" required /></label></div>
            <div v-else class="guided-question guided-complete"><div class="empty-icon">✦</div><h3>Creación guardada</h3><p>Tu personaje ya tiene sus valores iniciales. {{ guidedAwakened ? 'Los atributos seleccionados tienen +4 y los demás +3.' : guidedEinherjer === false ? 'Los atributos seleccionados tienen +1.' : 'La configuración inicial está guardada.' }}</p><button class="button button-primary" type="button" @click="closeModals">Cerrar</button></div>
          </div>
        </template>
      </section>
    </div>
    <div v-if="isDeleteCampaignModalOpen" class="modal-backdrop" @click.self="closeDeleteCampaignModal"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="delete-campaign-title"><button class="modal-close" aria-label="Cerrar" @click="closeDeleteCampaignModal">×</button><p class="eyebrow accent">CONFIRMAR BORRADO</p><h2 id="delete-campaign-title">¿Borrar campaña?</h2><p class="modal-copy">Se eliminará <strong>{{ selectedCampaign?.name }}</strong> y todos sus personajes. Esta acción no se puede deshacer.</p><div class="modal-actions"><button class="button button-quiet" type="button" @click="closeDeleteCampaignModal">Cancelar</button><button class="button button-danger" type="button" :disabled="saving" @click="deleteCampaign">{{ saving ? 'Borrando…' : 'Borrar definitivamente' }}</button></div></section></div>
  </main>
</template>
