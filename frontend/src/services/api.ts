const request = async (path: string, options: RequestInit = {}) => {
  const response = await fetch(path, { headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }, ...options });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || response.statusText);
  }
  if (response.status === 204) return null;
  return response.json();
};

export type AllocationPayload = {
  level: number;
  experience: number;
  attributes: Record<string, number>;
  genetics: Record<string, number>;
  minorAttributes: Record<string, number>;
  visible: boolean;
  final: boolean;
};

export const api = {
  campaigns: () => request('/api/campaigns'),
  createCampaign: (name: string) => request('/api/campaigns', { method: 'POST', body: JSON.stringify({ name }) }),
  deleteCampaign: (id: string) => request('/api/campaigns/' + id, { method: 'DELETE' }),
  campaign: (id: string) => request('/api/campaigns/' + id),
  minorAttributes: (id: string) => request('/api/campaigns/' + id + '/minor-attributes'),
  createMinorAttribute: (id: string, body: unknown) => request('/api/campaigns/' + id + '/minor-attributes', { method: 'POST', body: JSON.stringify(body) }),
  deleteMinorAttribute: (characterId: string, definitionId: string) => request('/api/characters/' + characterId + '/minor-attributes/' + definitionId, { method: 'DELETE' }),
  characters: (campaignId: string) => request('/api/campaigns/' + campaignId + '/characters'),
  createCharacter: (campaignId: string, body: unknown) => request('/api/campaigns/' + campaignId + '/characters', { method: 'POST', body: JSON.stringify(body) }),
  list: () => request('/api/characters'),
  create: (name: string) => request('/api/characters', { method: 'POST', body: JSON.stringify({ name }) }),
  get: (id: string) => request('/api/characters/' + id),
  deleteCharacter: (id: string) => request('/api/characters/' + id, { method: 'DELETE' }),
  pendingUniqueAbilities: (id: string) => request('/api/characters/' + id + '/unique-abilities/pending'),
  decideUniqueAbility: (id: string, name: string, decision: 'accepted' | 'rejected') => request('/api/characters/' + id + '/unique-abilities/' + encodeURIComponent(name) + '/decision', { method: 'POST', body: JSON.stringify({ decision }) }),
  edit: (id: string) => request('/api/characters/' + id + '/edit', { method: 'POST' }),
  addExperience: (id: string, amount: number) => request('/api/characters/' + id + '/experience', { method: 'POST', body: JSON.stringify({ amount }) }),
  levelUp: (id: string, body: AllocationPayload) => request('/api/characters/' + id + '/level-up', { method: 'POST', body: JSON.stringify(body) }),
  levelUpAll: (id: string, body: AllocationPayload) => request('/api/characters/' + id + '/level-up-all', { method: 'POST', body: JSON.stringify(body) }),
  attributeDetail: (characterId: string, key: string) => request('/api/characters/' + characterId + '/attributes/' + encodeURIComponent(key)),
  saveAttributeModifiers: (id: string, body: Record<string, { name: string; value: number }[]>) => request('/api/characters/' + id + '/attribute-modifiers', { method: 'PUT', body: JSON.stringify(body) }),
  save: (id: string, body: unknown) => request('/api/characters/' + id, { method: 'PUT', body: JSON.stringify(body) }),
  importLegacy: (id: string, code: string) => request('/api/characters/' + id + '/legacy/import', { method: 'POST', body: JSON.stringify({ code }) }),
  exportLegacy: async (id: string) => {
    const response = await fetch('/api/characters/' + id + '/legacy/export');
    if (!response.ok) throw new Error((await response.text()) || response.statusText);
    return response.text();
  },
  preview: (id: string, body: unknown) => request('/api/characters/' + id + '/preview', { method: 'POST', body: JSON.stringify(body) }),
  milestones: (id: string) => request('/api/characters/' + id + '/milestones'),
  cancelChanges: (id: string) => request('/api/characters/' + id + '/cancel-changes', { method: 'POST' }),
  recoverMilestone: (id: string, milestoneId: string) => request('/api/characters/' + id + '/history/' + encodeURIComponent(milestoneId) + '/recover', { method: 'POST' }),
  lastUpgrade: (id: string) => request('/api/characters/' + id + '/last-upgrade'),
  currentUpgrade: (id: string) => request('/api/characters/' + id + '/current-upgrade'),
  abilities: () => request('/api/abilities'),
  importAbilities: (body: unknown) => request('/api/abilities/import', { method: 'POST', body: JSON.stringify(body) }),
};
