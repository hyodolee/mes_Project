output "ec2_public_ip" {
  value       = aws_instance.mes_backend.public_ip
  description = "이 IP를 내도메인.한국 A레코드에 입력하세요"
}

output "ssh_command" {
  value = "ssh -i 키파일.pem ubuntu@${aws_instance.mes_backend.public_ip}"
}
