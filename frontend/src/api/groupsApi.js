import { apiRequest } from './client.js'

export async function getGroups() {
  return apiRequest('/groups')
}

export async function getGroup(groupId) {
  return apiRequest(`/groups/${groupId}`)
}

export async function createGroup(groupData) {
  return apiRequest('/groups', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(groupData),
  })
}

export async function joinGroup(joinCode) {
  return apiRequest('/groups/join', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ joinCode }),
  })
}
