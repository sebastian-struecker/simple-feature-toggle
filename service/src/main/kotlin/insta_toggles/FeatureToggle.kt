package insta_toggles

class FeatureToggle(
    val id: Long, val key: String, var name: String, var description: String, val contexts: List<Context>
) {
    init {
        checkInputs(key, name)
    }

    companion object {
        fun checkInputs(key: String, name: String) {
            require(key.matches(Regex("^[a-z_]*[a-z]$"))) {
                "Feature key '$key' is invalid"
            }
            require(name.isNotBlank()) {
                "Feature name must not be blank"
            }
        }
    }
}
