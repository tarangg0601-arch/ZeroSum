const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

export async function getGroups() {
  const response = await fetch(`${API_BASE_URL}/groups`)

  if (!response.ok) {
    throw new Error('Failed to fetch groups')
  }

  return response.json()
}

export async function getGroup(groupId) {
  const response = await fetch(`${API_BASE_URL}/groups/${groupId}`)

  if (!response.ok) {
    throw new Error('Failed to fetch group')
  }

  return response.json()
}

export async function createGroup(groupData) {
  const response = await fetch(`${API_BASE_URL}/groups`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(groupData),
  })

  if (!response.ok) {
    throw new Error('Failed to create group')
  }

  return response.json()
}

export async function createUser(userData) {
  const response = await fetch(`${API_BASE_URL}/users`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(userData),
  })
  const data = await response.json()
  if (!response.ok) throw new Error(data.error || data.message || 'Failed to create participant')
  return data
}
