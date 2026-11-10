package com.example.cambiateappkmp.data.repository

import com.example.cambiateappkmp.data.remote.CloudinaryConfig
import com.example.cambiateappkmp.data.remote.CloudinaryRespuesta
import com.example.cambiateappkmp.data.remote.LectorDeImagen
import com.example.cambiateappkmp.domain.repository.ImageStorageRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

class CloudinaryImageStorageRepository(
    private val cliente: HttpClient,
    private val lector: LectorDeImagen,
) : ImageStorageRepository {

    override suspend fun subirImagen(uri: String): Result<String> {
        return try {
            val bytes = lector.leerBytes(uri)

            val respuesta = cliente.submitFormWithBinaryData(
                url = "https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload",
                formData = formData {
                    append("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"foto.jpg\"")
                        },
                    )
                },
            )

            if (!respuesta.status.isSuccess()) {
                error("Cloudinary respondió ${respuesta.status}")
            }
            Result.success(respuesta.body<CloudinaryRespuesta>().secureUrl)
        } catch (e: CancellationException) {
            throw e // si se cancela la corrutina, no es un error de subida
        } catch (e: Exception) {
            println("Error subiendo imagen: $e")
            Result.failure(e)
        }
    }
}
