import React, { useState, useEffect } from "react";
import axios from "axios";

function App() {
  const [entries, setEntries] = useState([]);
  const [newEntry, setNewEntry] = useState({ name: "", groups: "", profile: "" });
  const [isEditing, setIsEditing] = useState(false);
  const [currentId, setCurrentId] = useState(null);

  useEffect(() => {
    // Fetch data from REST API
    axios
      .get("https://api.example.com/entries")
      .then((response) => {
        setEntries(response.data);
      })
      .catch((error) => console.log(error));
  }, []);

  const handleAddEntry = () => {
    axios
      .post("https://api.example.com/entries", {
        name: newEntry.name,
        groups: newEntry.groups, // No array handling here
        profile: newEntry.profile,
      })
      .then((response) => {
        setEntries([...entries, response.data]);
        setNewEntry({ name: "", groups: "", profile: "" });
      })
      .catch((error) => console.log(error));
  };

  const handleEditEntry = (entry) => {
    setIsEditing(true);
    setNewEntry({
      name: entry.name,
      groups: entry.groups, // Directly use groups as string
      profile: entry.profile,
    });
    setCurrentId(entry.id);
  };

  const handleUpdateEntry = () => {
    axios
      .put(`https://api.example.com/entries/${currentId}`, {
        name: newEntry.name,
        groups: newEntry.groups, // No array conversion needed
        profile: newEntry.profile,
      })
      .then((response) => {
        setEntries(
          entries.map((entry) =>
            entry.id === currentId ? response.data : entry
          )
        );
        setIsEditing(false);
        setNewEntry({ name: "", groups: "", profile: "" });
        setCurrentId(null);
      })
      .catch((error) => console.log(error));
  };

  const handleDeleteEntry = (id) => {
    axios
      .delete(`https://api.example.com/entries/${id}`)
      .then(() => {
        setEntries(entries.filter((entry) => entry.id !== id));
      })
      .catch((error) => console.log(error));
  };

  return (
    <div className="App">
      <h1>Entries</h1>
      <ul>
        {entries.map((entry) => (
          <li key={entry.id}>
            <strong>{entry.name}</strong> (Groups: {entry.groups})<br />
            {entry.profile}
            <button onClick={() => handleEditEntry(entry)}>Edit</button>
            <button onClick={() => handleDeleteEntry(entry.id)}>Delete</button>
          </li>
        ))}
      </ul>

      <h2>{isEditing ? "Edit Entry" : "Add Entry"}</h2>
      <input
        type="text"
        placeholder="Name"
        value={newEntry.name}
        onChange={(e) => setNewEntry({ ...newEntry, name: e.target.value })}
      />
      <input
        type="text"
        placeholder="Groups"
        value={newEntry.groups}
        onChange={(e) => setNewEntry({ ...newEntry, groups: e.target.value })}
      />
      <textarea
        placeholder="Profile"
        value={newEntry.profile}
        onChange={(e) => setNewEntry({ ...newEntry, profile: e.target.value })}
      />
      {isEditing ? (
        <button onClick={handleUpdateEntry}>Update Entry</button>
      ) : (
        <button onClick={handleAddEntry}>Add Entry</button>
      )}
    </div>
  );
}

export default App;
