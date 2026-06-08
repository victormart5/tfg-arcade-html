const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// 1. CONEXIÓN A MONGO (Local)
// arcadeDB es el nombre que le daremos a tu base de datos
mongoose.connect('mongodb://127.0.0.1:27017/arcadeDB')
    .then(() => console.log("✅ ¡Conectado a MongoDB con éxito!"))
    .catch(err => console.error("❌ Error al conectar a MongoDB:", err));

// 2. DEFINIR EL MODELO (Estructura de la sugerencia)
const SugerenciaSchema = new mongoose.Schema({
    usuario: String,
    comentario: String,
    fecha: { type: Date, default: Date.now }
});

const Sugerencia = mongoose.model('Sugerencia', SugerenciaSchema);

// 3. RUTAS API

// Obtener todas las sugerencias de la base de datos
app.get('/api/sugerencias', async (req, res) => {
    try {
        const lista = await Sugerencia.find().sort({ fecha: -1 });
        res.json(lista);
    } catch (error) {
        res.status(500).send("Error al obtener datos");
    }
});

// Guardar una nueva sugerencia
app.post('/api/sugerencias', async (req, res) => {
    try {
        const nueva = new Sugerencia({
            usuario: req.body.usuario,
            comentario: req.body.comentario
        });
        await nueva.save();
        res.json(nueva);
    } catch (error) {
        res.status(500).send("Error al guardar en la base de datos");
    }
});

// Arrancar servidor
app.listen(3000, () => console.log("🚀 Servidor corriendo en http://localhost:3000"));