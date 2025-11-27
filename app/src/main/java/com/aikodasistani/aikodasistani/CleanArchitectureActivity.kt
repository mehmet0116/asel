package com.aikodasistani.aikodasistani

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout

class CleanArchitectureActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clean_architecture)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.clean_architecture_title)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("Katmanlar"))
        tabLayout.addTab(tabLayout.newTab().setText("Prensipler"))
        tabLayout.addTab(tabLayout.newTab().setText("Örnek Yapı"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showLayers()
                    1 -> showPrinciples()
                    2 -> showExampleStructure()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        showLayers()
    }
    
    private fun showLayers() {
        recyclerView.adapter = LayersAdapter(getLayers())
    }
    
    private fun showPrinciples() {
        recyclerView.adapter = PrinciplesAdapter(getPrinciples())
    }
    
    private fun showExampleStructure() {
        recyclerView.adapter = StructureAdapter(getStructure())
    }
    
    private fun getLayers(): List<Layer> {
        return listOf(
            Layer(
                name = "🎯 Domain Layer (İş Mantığı)",
                description = "Uygulamanın çekirdeği. İş kurallarını ve entity'leri içerir. Hiçbir framework'e bağımlı değildir.",
                components = listOf(
                    "Entities - İş nesneleri",
                    "Use Cases - İş kuralları",
                    "Repository Interfaces - Soyut veri erişimi"
                ),
                code = """
// Domain Layer - Entity
data class User(
    val id: String,
    val name: String,
    val email: String
)

// Domain Layer - Use Case
class GetUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User> {
        return userRepository.getUserById(userId)
    }
}

// Domain Layer - Repository Interface
interface UserRepository {
    suspend fun getUserById(id: String): Result<User>
    suspend fun saveUser(user: User): Result<Unit>
}
                """.trimIndent(),
                color = "#4CAF50"
            ),
            Layer(
                name = "📦 Data Layer (Veri Katmanı)",
                description = "Veri kaynaklarına erişim sağlar. Repository implementasyonları, API servisleri ve veritabanı işlemleri burada yer alır.",
                components = listOf(
                    "Repository Implementations",
                    "Data Sources (Remote/Local)",
                    "DTOs ve Mappers",
                    "API Services",
                    "Database DAOs"
                ),
                code = """
// Data Layer - Repository Implementation
class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource
) : UserRepository {
    
    override suspend fun getUserById(id: String): Result<User> {
        return try {
            val userDto = remoteDataSource.getUser(id)
            Result.success(userDto.toDomain())
        } catch (e: Exception) {
            localDataSource.getUser(id)?.let {
                Result.success(it.toDomain())
            } ?: Result.failure(e)
        }
    }
}

// Data Layer - DTO
data class UserDto(
    val id: String,
    val name: String,
    val email: String
) {
    fun toDomain() = User(id, name, email)
}

// Data Layer - Remote Data Source
class UserRemoteDataSource(private val api: ApiService) {
    suspend fun getUser(id: String) = api.getUser(id)
}
                """.trimIndent(),
                color = "#2196F3"
            ),
            Layer(
                name = "🖥️ Presentation Layer (Sunum Katmanı)",
                description = "Kullanıcı arayüzü ile ilgili her şey. ViewModel, UI State, Activity/Fragment burada yer alır.",
                components = listOf(
                    "ViewModels",
                    "UI States",
                    "Activities/Fragments",
                    "Composables (Jetpack Compose)",
                    "UI Mappers"
                ),
                code = """
// Presentation Layer - UI State
sealed class UserUiState {
    object Loading : UserUiState()
    data class Success(val user: User) : UserUiState()
    data class Error(val message: String) : UserUiState()
}

// Presentation Layer - ViewModel
class UserViewModel(
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            getUserUseCase(userId)
                .onSuccess { user ->
                    _uiState.value = UserUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = UserUiState.Error(error.message ?: "Hata")
                }
        }
    }
}
                """.trimIndent(),
                color = "#9C27B0"
            ),
            Layer(
                name = "🔧 DI Layer (Dependency Injection)",
                description = "Bağımlılıkların yönetimi. Hilt, Koin veya manual DI ile bağımlılıklar sağlanır.",
                components = listOf(
                    "Modules (Hilt/Koin)",
                    "Component Definitions",
                    "Scope Annotations"
                ),
                code = """
// DI Layer - Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .build()
            .create(ApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(
        remoteDataSource: UserRemoteDataSource,
        localDataSource: UserLocalDataSource
    ): UserRepository {
        return UserRepositoryImpl(remoteDataSource, localDataSource)
    }
    
    @Provides
    fun provideGetUserUseCase(
        userRepository: UserRepository
    ): GetUserUseCase {
        return GetUserUseCase(userRepository)
    }
}
                """.trimIndent(),
                color = "#FF9800"
            )
        )
    }
    
    private fun getPrinciples(): List<Principle> {
        return listOf(
            Principle(
                name = "Bağımlılık Kuralı",
                icon = "⬆️",
                description = "Bağımlılıklar her zaman dıştan içe doğru olmalıdır. İç katmanlar dış katmanları bilmemelidir.",
                details = """
• Domain → Hiçbir şeye bağımlı değil
• Data → Domain'e bağımlı
• Presentation → Domain'e bağımlı
• DI → Hepsine bağımlı (sadece DI katmanında)

❌ Domain ASLA Data veya Presentation'a bağımlı olamaz!
                """.trimIndent()
            ),
            Principle(
                name = "Test Edilebilirlik",
                icon = "🧪",
                description = "Her katman bağımsız olarak test edilebilir olmalıdır.",
                details = """
• Unit Tests: Use Cases, ViewModels
• Integration Tests: Repositories
• UI Tests: Screens/Composables

Repository Interface sayesinde Domain layer
tamamen mock'lanabilir!
                """.trimIndent()
            ),
            Principle(
                name = "Separation of Concerns",
                icon = "📦",
                description = "Her katman kendi sorumluluğuna odaklanmalıdır.",
                details = """
• Domain: İş kuralları
• Data: Veri erişimi ve dönüşümü
• Presentation: UI durumu ve kullanıcı etkileşimi
• DI: Bağımlılık sağlama

Bir katmandaki değişiklik diğerlerini etkilemez!
                """.trimIndent()
            ),
            Principle(
                name = "Framework Bağımsızlığı",
                icon = "🔓",
                description = "Domain katmanı hiçbir framework'e bağımlı olmamalıdır.",
                details = """
• Android SDK yok
• Room yok
• Retrofit yok
• Sadece pure Kotlin/Java

Bu sayede domain logic
herhangi bir platformda çalışabilir!
                """.trimIndent()
            )
        )
    }
    
    private fun getStructure(): List<FolderItem> {
        return listOf(
            FolderItem("📁 app/", 0, "Ana modül"),
            FolderItem("├── 📁 di/", 1, "Dependency Injection"),
            FolderItem("│   ├── AppModule.kt", 2, "Singleton bağımlılıklar"),
            FolderItem("│   ├── NetworkModule.kt", 2, "Ağ bağımlılıkları"),
            FolderItem("│   └── DatabaseModule.kt", 2, "Veritabanı bağımlılıkları"),
            FolderItem("├── 📁 domain/", 1, "İş Mantığı Katmanı"),
            FolderItem("│   ├── 📁 model/", 2, "Entity'ler"),
            FolderItem("│   │   ├── User.kt", 3, ""),
            FolderItem("│   │   └── Product.kt", 3, ""),
            FolderItem("│   ├── 📁 repository/", 2, "Repository Interface'leri"),
            FolderItem("│   │   ├── UserRepository.kt", 3, ""),
            FolderItem("│   │   └── ProductRepository.kt", 3, ""),
            FolderItem("│   └── 📁 usecase/", 2, "Use Case'ler"),
            FolderItem("│       ├── GetUserUseCase.kt", 3, ""),
            FolderItem("│       ├── SaveUserUseCase.kt", 3, ""),
            FolderItem("│       └── GetProductsUseCase.kt", 3, ""),
            FolderItem("├── 📁 data/", 1, "Veri Katmanı"),
            FolderItem("│   ├── 📁 repository/", 2, "Repository Impl"),
            FolderItem("│   │   └── UserRepositoryImpl.kt", 3, ""),
            FolderItem("│   ├── 📁 remote/", 2, "API Servisleri"),
            FolderItem("│   │   ├── ApiService.kt", 3, ""),
            FolderItem("│   │   ├── UserRemoteDataSource.kt", 3, ""),
            FolderItem("│   │   └── 📁 dto/", 3, "Data Transfer Objects"),
            FolderItem("│   │       └── UserDto.kt", 4, ""),
            FolderItem("│   └── 📁 local/", 2, "Yerel Veritabanı"),
            FolderItem("│       ├── AppDatabase.kt", 3, ""),
            FolderItem("│       ├── UserDao.kt", 3, ""),
            FolderItem("│       └── 📁 entity/", 3, "Room Entity'leri"),
            FolderItem("│           └── UserEntity.kt", 4, ""),
            FolderItem("└── 📁 presentation/", 1, "Sunum Katmanı"),
            FolderItem("    ├── 📁 user/", 2, "User Feature"),
            FolderItem("    │   ├── UserViewModel.kt", 3, ""),
            FolderItem("    │   ├── UserUiState.kt", 3, ""),
            FolderItem("    │   └── UserScreen.kt", 3, ""),
            FolderItem("    └── 📁 common/", 2, "Ortak UI Bileşenleri"),
            FolderItem("        └── LoadingIndicator.kt", 3, "")
        )
    }
    
    data class Layer(
        val name: String,
        val description: String,
        val components: List<String>,
        val code: String,
        val color: String
    )
    
    data class Principle(
        val name: String,
        val icon: String,
        val description: String,
        val details: String
    )
    
    data class FolderItem(
        val name: String,
        val level: Int,
        val description: String
    )
    
    inner class LayersAdapter(private val layers: List<Layer>) : 
        RecyclerView.Adapter<LayersAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.nameText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
            val componentsText: TextView = view.findViewById(R.id.componentsText)
            val codeText: TextView = view.findViewById(R.id.codeText)
            val copyBtn: View = view.findViewById(R.id.copyBtn)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_clean_layer, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val layer = layers[position]
            holder.nameText.text = layer.name
            holder.descriptionText.text = layer.description
            holder.componentsText.text = layer.components.joinToString("\n") { "• $it" }
            holder.codeText.text = layer.code
            
            holder.copyBtn.setOnClickListener {
                copyToClipboard(layer.code)
            }
        }
        
        override fun getItemCount() = layers.size
    }
    
    inner class PrinciplesAdapter(private val principles: List<Principle>) :
        RecyclerView.Adapter<PrinciplesAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconText: TextView = view.findViewById(R.id.iconText)
            val nameText: TextView = view.findViewById(R.id.nameText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
            val detailsText: TextView = view.findViewById(R.id.detailsText)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_clean_principle, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val principle = principles[position]
            holder.iconText.text = principle.icon
            holder.nameText.text = principle.name
            holder.descriptionText.text = principle.description
            holder.detailsText.text = principle.details
        }
        
        override fun getItemCount() = principles.size
    }
    
    inner class StructureAdapter(private val items: List<FolderItem>) :
        RecyclerView.Adapter<StructureAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.nameText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_folder_structure, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.nameText.text = item.name
            holder.descriptionText.text = item.description
            holder.descriptionText.visibility = if (item.description.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        override fun getItemCount() = items.size
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Kopyalandı", Toast.LENGTH_SHORT).show()
    }
}
