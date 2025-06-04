package views

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import com.example.android.camera2.basic.R
import com.example.android.camera2.basic.databinding.ActivityViewFinderBinding
import view_model.CamaraViewModel

class ViewFinderActivity : AppCompatActivity() {
    private val vm: CamaraViewModel by viewModels()
    private lateinit var binding: ActivityViewFinderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewFinderBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        vm.init(this,binding.viewFinder)
    }
}


//ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//    insets
//}