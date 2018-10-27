package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class OtherModule {

    @Singleton
    @Provides
    fun kryoPool(): Pool<Kryo> {
        return object : Pool<Kryo>(true, true) {
            override fun create(): Kryo = Kryo()
        }
    }

}