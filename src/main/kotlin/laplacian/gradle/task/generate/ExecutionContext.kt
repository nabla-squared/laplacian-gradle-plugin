package laplacian.gradle.task.generate

import com.github.jknack.handlebars.Context
import laplacian.handlebars.TemplateWrapper
import laplacian.util.YamlLoader
import laplacian.util.mergeObjectGraph
import org.yaml.snakeyaml.Yaml
import java.io.File

class ExecutionContext(
    private var entries: Map<String, Any?> = emptyMap(),
    private var modelEntryResolvers: List<ModelEntryResolver> = mutableListOf(),
    private var modelSchema: File? = null
) {
    lateinit var baseModel: Context
    lateinit var currentTemplate: File
    lateinit var currentTarget: File
    var currentContent: String? = null

    fun addModelEntryResolver(vararg resolvers: ModelEntryResolver): ExecutionContext {
        modelEntryResolvers = (modelEntryResolvers + resolvers).distinct()
        return this
    }

    fun addModel(vararg models: String): ExecutionContext {
        entries = models.fold(entries) { acc, model ->
            try {
                val readModel = Yaml().load<Map<String, Any?>>(model)
                mergeObjectGraph(acc, readModel) as Map<String, Any?>
            }
            catch (e: RuntimeException) {
                throw IllegalStateException(
                    "A problem occurred while merging the model: $model", e
                )
            }
        }
        return this
    }

    fun setModelSchema(schemaFile: File): ExecutionContext {
        modelSchema = schemaFile
        return this
    }

    fun addModel(vararg modelFiles: File): ExecutionContext {
        entries = YamlLoader.readObjects(modelFiles.toList(), modelSchema, baseModel = entries)
        return this
    }

    fun build(): ExecutionContext {
        baseModel = TemplateWrapper.createContext(entries, *modelEntryResolvers.map { resolver ->
            ModelEntryResolverBridge(resolver, this)
        }.toTypedArray())
        return this
    }

    private var _currentModel: Context? = null
    var currentModel: Context
        get() = _currentModel ?: baseModel
        set(model) {
            _currentModel = model
        }
}
