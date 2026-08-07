<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from './services/api';
import CharacterSheet from './CharacterSheet.vue';

type Campaign = { id: string; name: string; createdAt: string };
type Character = { id: string; name: string; imageUrl?: string | null; campaignId?: string };

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
const loading = ref(true);
const saving = ref(false);
const error = ref('');
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
function openCampaignModal() { campaignName.value = ''; isCampaignModalOpen.value = true; }
function openCharacterModal() { characterName.value = ''; characterImage.value = ''; isCharacterModalOpen.value = true; }
function closeModals() { isCampaignModalOpen.value = false; isCharacterModalOpen.value = false; }
function openDeleteCampaignModal() { isDeleteCampaignModalOpen.value = true; }
function closeDeleteCampaignModal() { isDeleteCampaignModalOpen.value = false; }
async function createCampaign() {
  if (!campaignName.value.trim()) return;
  saving.value = true;
  try { const campaign = await api.createCampaign(campaignName.value.trim()); await loadCampaigns(); await selectCampaign(campaign); closeModals(); } catch (e: any) { error.value = e.message; } finally { saving.value = false; }
}
async function createCharacter() {
  if (!selectedCampaign.value || !characterName.value.trim()) return;
  saving.value = true;
  try { await api.createCharacter(selectedCampaign.value.id, { name: characterName.value.trim(), imageUrl: characterImage.value || null }); characters.value = await api.characters(selectedCampaign.value.id); closeModals(); } catch (e: any) { error.value = e.message; } finally { saving.value = false; }
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
  if (campaign && selectedCampaign.value?.id !== campaign.id) selectCampaign(campaign);
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
        <button class="new-campaign-link" @click="openCampaignModal">＋ Crear una campaña</button>
      </aside>

      <section v-if="selectedCampaign" class="campaign-view">
        <div class="campaign-heading"><div><p class="eyebrow accent">CAMPAÑA SELECCIONADA</p><h2>{{ selectedCampaign.name }}</h2><p class="muted">{{ characters.length }} {{ characters.length === 1 ? 'personaje' : 'personajes' }} en esta aventura</p></div><div class="campaign-actions"><button class="button button-danger" @click="openDeleteCampaignModal">Borrar campaña</button><button class="button button-primary" @click="openCharacterModal"><span>＋</span> Nuevo personaje</button></div></div>
        <div v-if="characters.length" class="character-grid"><article v-for="character in characters" :key="character.id" class="character-card" role="link" tabindex="0" @click="openCharacter(character)" @keydown.enter="openCharacter(character)"><div class="portrait"><img v-if="character.imageUrl" :src="character.imageUrl" :alt="`Retrato de ${character.name}`" /><span v-else>{{ initials(character.name) }}</span></div><div class="character-info"><p class="eyebrow">PERSONAJE</p><h3>{{ character.name }}</h3><span class="card-link">Ver ficha →</span></div></article></div>
        <div v-else class="empty-state"><div class="empty-icon">✦</div><h3>La aventura está esperando</h3><p>Añade el primer personaje a <strong>{{ selectedCampaign.name }}</strong>.</p><button class="button button-primary" @click="openCharacterModal">＋ Nuevo personaje</button></div>
      </section>
      <section v-else class="welcome-state"><div class="welcome-orbit">✦</div><p class="eyebrow accent">PRIMER PASO</p><h2>Selecciona una campaña</h2><p>Elige un mundo del archivo o crea uno nuevo para comenzar.</p><button class="button button-primary" @click="openCampaignModal">＋ Nueva campaña</button></section>
    </div>

    <div v-if="isCampaignModalOpen || isCharacterModalOpen" class="modal-backdrop" @click.self="closeModals"><section class="modal" role="dialog" aria-modal="true" :aria-labelledby="isCampaignModalOpen ? 'campaign-modal-title' : 'character-modal-title'"><button class="modal-close" aria-label="Cerrar" @click="closeModals">×</button><p class="eyebrow accent">{{ isCampaignModalOpen ? 'NUEVO MUNDO' : 'NUEVO PERSONAJE' }}</p><h2 :id="isCampaignModalOpen ? 'campaign-modal-title' : 'character-modal-title'">{{ isCampaignModalOpen ? 'Crea una campaña' : 'Da vida a tu personaje' }}</h2><p class="modal-copy">{{ isCampaignModalOpen ? 'Un nombre basta para abrir un nuevo capítulo.' : 'Guarda el nombre y el retrato que reconocerás en la mesa.' }}</p><form @submit.prevent="isCampaignModalOpen ? createCampaign() : createCharacter()"><label>Nombre<input v-if="isCampaignModalOpen" v-model="campaignName" autofocus placeholder="Ej. Las cenizas de Midgard" maxlength="160" /><input v-else v-model="characterName" autofocus placeholder="Ej. Astrid la Errante" maxlength="160" /></label><template v-if="isCharacterModalOpen"><label>Imagen <span class="label-hint">URL o archivo local</span><input v-model="characterImage" placeholder="https://…" /></label><label class="file-input">Subir retrato<input ref="imageInput" type="file" accept="image/*" @change="readImage" /></label><div v-if="characterImage" class="image-preview"><img :src="characterImage" alt="Vista previa del retrato" /><button type="button" @click="clearImage">Quitar imagen</button></div></template><button class="button button-primary modal-submit" :disabled="saving || (isCampaignModalOpen ? !campaignName.trim() : !characterName.trim())">{{ saving ? 'Guardando…' : (isCampaignModalOpen ? 'Crear campaña' : 'Crear personaje') }}</button></form></section></div>
    <div v-if="isDeleteCampaignModalOpen" class="modal-backdrop" @click.self="closeDeleteCampaignModal"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="delete-campaign-title"><button class="modal-close" aria-label="Cerrar" @click="closeDeleteCampaignModal">×</button><p class="eyebrow accent">CONFIRMAR BORRADO</p><h2 id="delete-campaign-title">¿Borrar campaña?</h2><p class="modal-copy">Se eliminará <strong>{{ selectedCampaign?.name }}</strong> y todos sus personajes. Esta acción no se puede deshacer.</p><div class="modal-actions"><button class="button button-quiet" type="button" @click="closeDeleteCampaignModal">Cancelar</button><button class="button button-danger" type="button" :disabled="saving" @click="deleteCampaign">{{ saving ? 'Borrando…' : 'Borrar definitivamente' }}</button></div></section></div>
  </main>
</template>
