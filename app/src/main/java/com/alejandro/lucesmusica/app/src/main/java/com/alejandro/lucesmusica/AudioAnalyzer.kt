        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
    }
}
